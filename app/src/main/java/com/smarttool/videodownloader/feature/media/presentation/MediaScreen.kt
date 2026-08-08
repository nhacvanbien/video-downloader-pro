package com.smarttool.videodownloader.feature.media.presentation

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.RetainedAndroidView
import com.smarttool.videodownloader.core.ui.theme.AppBlack
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Primary
import java.util.concurrent.TimeUnit

/**
 * Video playback chrome. [playerView] is the ExoPlayer `PlayerView` the activity owns
 * — Compose has no player surface, so the real View is hosted via [RetainedAndroidView] and
 * only the controls around it are composed.
 */
@Composable
fun MediaScreen(
    state: MediaContract.State,
    playerView: View,
    onBack: () -> Unit,
    onMore: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleFill: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        if (!state.fillMode) {
            MediaHeader(
                title = state.title,
                showMoreAction = state.showMoreAction,
                onBack = onBack,
                onMore = onMore,
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            RetainedAndroidView(view = playerView, modifier = Modifier.fillMaxSize())
        }

        MediaControls(
            state = state,
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onCycleSpeed = onCycleSpeed,
            onToggleLoop = onToggleLoop,
            onToggleFill = onToggleFill,
        )
    }
}

@Composable
private fun MediaHeader(
    title: String,
    showMoreAction: Boolean,
    onBack: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = null,
            modifier = Modifier.size(28.dp).clickable(onClick = onBack).padding(4.dp),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = AppWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )

        if (showMoreAction) {
            Image(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = null,
                modifier = Modifier.size(22.dp).clickable(onClick = onMore),
            )
        }
    }
}

@Composable
private fun MediaControls(
    state: MediaContract.State,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleFill: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Slider(
            value = state.positionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
            ),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${formatDuration(state.positionMs)} / ${formatDuration(state.durationMs)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = AppWhite,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = "${state.speed}x",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = AppWhite,
                modifier = Modifier.clickable(onClick = onCycleSpeed).padding(8.dp),
            )

            Image(
                painter = painterResource(
                    if (state.looping) R.drawable.ic_enable_loop else R.drawable.ic_disable_loop_white,
                ),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(22.dp)
                    .clickable(onClick = onToggleLoop),
            )

            Image(
                painter = painterResource(
                    if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_media,
                ),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(32.dp)
                    .clickable(onClick = onPlayPause),
            )

            Image(
                painter = painterResource(if (state.fillMode) R.drawable.ic_fit else R.drawable.ic_fill),
                contentDescription = null,
                modifier = Modifier.size(22.dp).clickable(onClick = onToggleFill),
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
