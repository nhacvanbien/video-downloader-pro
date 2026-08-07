package com.smarttool.videodownloader.core.ui.components

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hosts a View whose instance outlives the composition — a WebView, an ExoPlayer
 * `PlayerView`, or the ad SDK's banner container.
 *
 * A plain `AndroidView(factory = { view })` crashes the second time a destination is
 * composed: disposing the first composition leaves the View parented to the discarded
 * holder, so re-adding it throws "The specified child already has a parent". Detaching
 * inside the factory makes the same instance re-hostable across navigations and tab
 * switches, which is what keeps page state and playback alive.
 */
@Composable
fun RetainedAndroidView(view: View, modifier: Modifier = Modifier) {
    AndroidView(
        factory = {
            (view.parent as? ViewGroup)?.removeView(view)
            view
        },
        modifier = modifier,
    )
}
