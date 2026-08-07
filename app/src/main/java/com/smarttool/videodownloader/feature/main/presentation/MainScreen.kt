package com.smarttool.videodownloader.feature.main.presentation

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.smarttool.videodownloader.core.ui.components.RetainedAndroidView
import com.smarttool.videodownloader.core.ui.theme.AppWhite

/**
 * Bottom-navigation shell. [tabContent] renders the body of [selectedTab]; the tabs are
 * plain composables now, so anything that has to survive a tab switch (the processing
 * screen's detection WebView) is owned by the Activity rather than the composition.
 * [bannerAd] is the ad SDK's own View.
 */
@Composable
fun MainScreen(
    selectedTab: MainTab,
    bannerAd: View,
    onSelectTab: (MainTab) -> Unit,
    tabContent: @Composable (MainTab) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(AppWhite)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            tabContent(selectedTab)
        }

        MainBottomBar(selected = selectedTab, onSelect = onSelectTab)

        RetainedAndroidView(view = bannerAd, modifier = Modifier.fillMaxWidth())
    }
}
