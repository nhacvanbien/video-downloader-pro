package com.smarttool.videodownloader.feature.guide.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
fun GuideRoute(
    isHomeGuide: Boolean,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    GuideScreen(isHomeGuide = isHomeGuide, onBack = onBack)
}
