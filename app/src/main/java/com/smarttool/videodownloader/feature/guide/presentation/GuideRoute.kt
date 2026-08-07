package com.smarttool.videodownloader.feature.guide.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Leaving the guide is an interstitial slot, as it was when this was an Activity —
 * hence [showInterstitial] wrapping every exit path rather than a bare [onBack].
 */
@Composable
fun GuideRoute(
    isHomeGuide: Boolean,
    showInterstitial: (onDone: () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val leave = { showInterstitial(onBack) }

    BackHandler { leave() }

    GuideScreen(isHomeGuide = isHomeGuide, onBack = leave)
}
