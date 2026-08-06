package com.smarttool.videodownloader.feature.main.presentation

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.smarttool.videodownloader.core.ui.theme.AppWhite

/**
 * Bottom-navigation shell. [tabContent] is the `FragmentContainerView` the activity
 * still swaps tab fragments into — the tabs remain Fragments because Processing owns
 * a WebView and permission callbacks that are bound to the fragment lifecycle.
 * [bannerAd] is the ad SDK's own View.
 */
@Composable
fun MainScreen(
    selectedTab: MainTab,
    tabContent: View,
    bannerAd: View,
    onSelectTab: (MainTab) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(AppWhite)) {
        AndroidView(
            factory = { tabContent },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        MainBottomBar(selected = selectedTab, onSelect = onSelectTab)

        AndroidView(factory = { bannerAd }, modifier = Modifier.fillMaxWidth())
    }
}
