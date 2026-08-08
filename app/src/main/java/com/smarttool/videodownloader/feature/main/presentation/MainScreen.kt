package com.smarttool.videodownloader.feature.main.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.smarttool.videodownloader.core.ui.components.ScreenGradient

@Composable
fun MainScreen(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    tabContent: @Composable (MainTab) -> Unit,
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(ScreenGradient)
        .statusBarsPadding()) {
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
            tabContent(selectedTab)
        }
        MainBottomBar(selected = selectedTab, onSelect = onSelectTab)
    }
}
