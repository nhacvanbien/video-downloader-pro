package com.smarttool.videodownloader.feature.media.presentation

import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecyclePauseOrDisposeEffectResult
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.core.ui.components.findComponentActivity
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.koin.androidx.compose.koinViewModel

/**
 * Video/image playback.
 *
 * Fill mode flips the Activity to landscape, which is why the orientation is restored
 * on the way out — the host Activity is shared with every other destination now, so a
 * leftover landscape lock would follow the user back to the library.
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

    LaunchedEffect(url) { viewModel.load(title = title, showMoreAction = isDownloaded) }

    val headers = remember(headersJson) { parseHeaders(headersJson) }

    val holder = remember(url) {
        val settings = viewModel.uiState.value

        MediaPlayerHolder(
            context = context,
            url = Uri.parse(url),
            headers = headers,
            speed = settings.speed,
            fillMode = settings.fillMode,
            looping = { viewModel.uiState.value.looping },
            onPlayingChanged = viewModel::onPlaybackStateChanged,
        )
    }

    DisposableEffect(holder) {
        onDispose { holder.release() }
    }

    DisposableEffect(Unit) {
        onDispose { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }

    LaunchedEffect(holder) {
        while (true) {
            viewModel.onProgress(
                positionMs = holder.player.currentPosition,
                durationMs = holder.player.duration.coerceAtLeast(0),
            )
            delay(PROGRESS_INTERVAL_MILLIS)
        }
    }

    LifecycleResumeEffect(holder) {
        onPauseOrDispose {
            holder.player.pause()
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.fillMode) {
        activity.requestedOrientation = if (state.fillMode) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    MediaScreen(
        state = state,
        playerView = holder.playerView,
        onBack = onBack,
        onMore = { /* detail sheet is opened from the library screens */ },
        onPlayPause = holder::togglePlayback,
        onSeek = { holder.player.seekTo(it) },
        onCycleSpeed = { holder.player.setPlaybackSpeed(viewModel.cycleSpeed()) },
        onToggleLoop = { viewModel.toggleLooping() },
        onToggleFill = { holder.setFillMode(viewModel.toggleFillMode()) },
    )
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
