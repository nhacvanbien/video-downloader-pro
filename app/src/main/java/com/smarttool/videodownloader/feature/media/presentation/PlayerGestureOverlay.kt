package com.smarttool.videodownloader.feature.media.presentation

import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.core.ui.components.findComponentActivity
import com.smarttool.videodownloader.core.ui.theme.ShapeMd

private enum class GestureMode { BRIGHTNESS, VOLUME }

/**
 * P2: invisible layer over the player surface — swipe the left half to adjust screen
 * brightness, the right half for media volume, matching the platform convention (YouTube,
 * most gallery/player apps). No permanent chrome; only a transient readout while dragging.
 */
@Composable
fun PlayerGestureOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = remember { context.findComponentActivity() }
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }

    var overlayLabel by remember { mutableStateOf<String?>(null) }
    var dragMode by remember { mutableStateOf<GestureMode?>(null) }
    var currentBrightness by remember { mutableStateOf(currentScreenBrightness(activity)) }
    var currentVolume by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }

    // Leaving the player must not leave the window's brightness pinned away from the
    // system default for every other screen.
    DisposableEffect(Unit) {
        onDispose { setScreenBrightness(activity, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        dragMode = if (offset.x < size.width / 2) {
                            GestureMode.BRIGHTNESS
                        } else {
                            GestureMode.VOLUME
                        }
                    },
                    onDragEnd = {
                        dragMode = null
                        overlayLabel = null
                    },
                    onDragCancel = {
                        dragMode = null
                        overlayLabel = null
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val fraction = -dragAmount / size.height.coerceAtLeast(1)

                        when (dragMode) {
                            GestureMode.BRIGHTNESS -> {
                                currentBrightness = (currentBrightness + fraction).coerceIn(0.02f, 1f)
                                setScreenBrightness(activity, currentBrightness)
                                overlayLabel = "${(currentBrightness * 100).toInt()}%"
                            }

                            GestureMode.VOLUME -> {
                                val delta = (fraction * maxVolume).toInt()
                                if (delta != 0) {
                                    currentVolume = (currentVolume + delta).coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        currentVolume,
                                        0,
                                    )
                                }
                                overlayLabel = "${currentVolume * 100 / maxVolume}%"
                            }

                            null -> Unit
                        }
                    },
                )
            },
    ) {
        overlayLabel?.let { label ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(ShapeMd)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(text = label, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun currentScreenBrightness(activity: ComponentActivity): Float {
    val value = activity.window.attributes.screenBrightness
    return if (value in 0f..1f) value else 0.5f
}

private fun setScreenBrightness(activity: ComponentActivity, value: Float) {
    val layoutParams = activity.window.attributes
    layoutParams.screenBrightness = value
    activity.window.attributes = layoutParams
}
