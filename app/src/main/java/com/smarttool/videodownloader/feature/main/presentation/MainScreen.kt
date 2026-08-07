package com.smarttool.videodownloader.feature.main.presentation

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.core.ui.components.RetainedAndroidView
import com.smarttool.videodownloader.core.ui.components.ScreenGradient
import com.smarttool.videodownloader.core.ui.theme.AppWhite

@Composable
fun MainScreen(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    tabContent: @Composable (MainTab) -> Unit,
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(ScreenGradient)) {
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars))
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
            tabContent(selectedTab)
        }
        MainBottomBar(selected = selectedTab, onSelect = onSelectTab)
    }
}
