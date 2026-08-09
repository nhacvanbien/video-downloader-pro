package com.smarttool.videodownloader.feature.media.presentation

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.file.IntentUtil
import com.smarttool.videodownloader.core.ui.SystemUiController
import com.smarttool.videodownloader.core.ui.components.findComponentActivity
import com.smarttool.videodownloader.core.ui.dialogs.DialogConfirmDelete
import com.smarttool.videodownloader.core.ui.dialogs.DialogRename
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File

/**
 * Video/image playback.
 *
 * The Activity is shared with every other destination now, so orientation and system
 * bars are always restored on the way out — a leftover landscape lock or hidden status
 * bar would otherwise follow the user back to the library.
 */
@Composable
fun MediaRoute(
    url: String,
    title: String,
    isDownloaded: Boolean,
    headersJson: String,
    onBack: () -> Unit,
) {
    val viewModel: MediaViewModel = koinViewModel()
    val context = LocalContext.current
    val activity = context.findComponentActivity()
    val systemUiController: SystemUiController = koinInject()
    val intentUtil: IntentUtil = koinInject()

    LaunchedEffect(url) {
        viewModel.onEvent(
            MediaContract.Event.Load(title = title, showMoreAction = isDownloaded, filePath = url),
        )
    }

    val headers = remember(headersJson) { parseHeaders(headersJson) }

    val holder = remember(url) {
        val settings = viewModel.uiState.value

        // isDownloaded means `url` is a raw filesystem path (from VideoTaskItem.filePath),
        // not an escaped URI. Titles routinely contain '#' or '?' (e.g. Facebook captions
        // with hashtags), which Uri.parse() would read as a fragment/query delimiter and
        // truncate the path, breaking playback with a FileNotFoundException.
        val mediaUri = if (isDownloaded) Uri.fromFile(File(url)) else Uri.parse(url)

        MediaPlayerHolder(
            context = context,
            url = mediaUri,
            headers = headers,
            speed = settings.speed,
            fillMode = settings.fillMode,
            looping = { viewModel.uiState.value.looping },
            onPlayingChanged = { viewModel.onEvent(MediaContract.Event.PlaybackStateChanged(it)) },
        )
    }

    DisposableEffect(holder) {
        onDispose { holder.release() }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            systemUiController.showSystemUI(activity.window, activity.window.decorView)
        }
    }

    LaunchedEffect(holder) {
        while (true) {
            viewModel.onEvent(
                MediaContract.Event.Progress(
                    positionMs = holder.player.currentPosition,
                    durationMs = holder.player.duration.coerceAtLeast(0),
                ),
            )
            delay(PROGRESS_INTERVAL_MILLIS)
        }
    }

    // Speed/fill-mode changes are one-shot player mutations, not re-render-driven state —
    // collecting them as effects (rather than reacting to uiState) keeps the initial
    // Load-driven state set and unrelated recompositions from re-applying them.
    LaunchedEffect(holder) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MediaContract.Effect.SpeedChanged -> holder.player.setPlaybackSpeed(effect.speed)
                is MediaContract.Effect.LoopingChanged -> Unit // read live via the `looping` lambda above
                is MediaContract.Effect.FillModeChanged -> holder.setFillMode(effect.fillMode)
                MediaContract.Effect.Deleted -> onBack()
                MediaContract.Effect.RenameFailed -> Toast.makeText(
                    context,
                    context.getString(R.string.string_invalid_data),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    LifecycleResumeEffect(holder) {
        onPauseOrDispose {
            holder.player.pause()
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // `isFullscreen` only forces a lock while true; releasing it hands rotation back to
    // the sensor instead of pinning to portrait, so a phone still physically held
    // landscape stays landscape rather than fighting the user back to portrait.
    LaunchedEffect(state.isFullscreen) {
        activity.requestedOrientation = if (state.isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isLandscape) {
        if (isLandscape) {
            systemUiController.hideSystemUI(activity.window, activity.window.decorView)
        } else {
            systemUiController.showSystemUI(activity.window, activity.window.decorView)
        }
    }

    MediaScreen(
        state = state,
        playerView = holder.playerView,
        onBack = onBack,
        onMore = { viewModel.onEvent(MediaContract.Event.ShowMoreMenu) },
        onPlayPause = holder::togglePlayback,
        onSeek = { holder.player.seekTo(it) },
        onCycleSpeed = { viewModel.onEvent(MediaContract.Event.CycleSpeed) },
        onToggleLoop = { viewModel.onEvent(MediaContract.Event.ToggleLooping) },
        onToggleFill = { viewModel.onEvent(MediaContract.Event.ToggleFillMode) },
        onToggleFullscreen = { viewModel.onEvent(MediaContract.Event.ToggleFullscreen) },
    )

    if (state.moreMenuVisible) {
        state.item?.let { item ->
            MediaMoreSheet(
                fileName = item.fileName,
                onRename = {
                    viewModel.onEvent(MediaContract.Event.HideMoreMenu)
                    DialogRename(context, item.fileName) { newName ->
                        viewModel.onEvent(MediaContract.Event.Rename(context, newName))
                    }.show()
                },
                onShare = {
                    viewModel.onEvent(MediaContract.Event.HideMoreMenu)
                    intentUtil.shareVideo(context, File(item.filePath).toUri())
                },
                onDelete = {
                    viewModel.onEvent(MediaContract.Event.HideMoreMenu)
                    DialogConfirmDelete(context) {
                        viewModel.onEvent(MediaContract.Event.Delete)
                    }.show()
                },
                onDismiss = { viewModel.onEvent(MediaContract.Event.HideMoreMenu) },
            )
        }
    }
}

private fun parseHeaders(raw: String): Map<String, String> {
    if (raw.isEmpty()) return emptyMap()

    return runCatching {
        Json.parseToJsonElement(raw).jsonObject
            .map { it.key to it.value.toString() }
            .toMap()
    }.getOrDefault(emptyMap())
}

private const val PROGRESS_INTERVAL_MILLIS = 500L
