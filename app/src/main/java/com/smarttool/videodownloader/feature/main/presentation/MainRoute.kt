package com.smarttool.videodownloader.feature.main.presentation

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.feature.browser.presentation.BrowserHomeRoute
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingRoute
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingWebViewHost
import com.smarttool.videodownloader.feature.library.presentation.LibraryRoute
import com.smarttool.videodownloader.feature.settings.presentation.SettingsRoute

/**
 * The four-tab home. It is the graph's start destination, so back here means "leave the
 * app" — hence the [onExitRequested] hook that puts up the exit dialog.
 */
@Composable
fun MainRoute(
    selectedTab: MainTab,
    bannerAd: View,
    processingHost: ProcessingWebViewHost,
    onSelectTab: (MainTab) -> Unit,
    showInterstitial: (onDone: () -> Unit) -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenGuide: (from: String) -> Unit,
    onOpenTabs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPrivateArea: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenMedia: (VideoTaskItem) -> Unit,
    onPreviewMedia: (url: String, title: String, headers: String) -> Unit,
    onExitRequested: () -> Unit,
) {
    BackHandler { onExitRequested() }

    MainScreen(
        selectedTab = selectedTab,
        onSelectTab = onSelectTab,
    ) { tab ->
        when (tab) {
            MainTab.Browser -> BrowserHomeRoute(
                showInterstitial = showInterstitial,
                onOpenUrl = onOpenUrl,
                onOpenGuide = { onOpenGuide(GUIDE_FROM_HOME) },
                onOpenTabs = onOpenTabs,
                onOpenBookmarks = onOpenBookmarks,
                onOpenHistory = onOpenHistory,
            )

            MainTab.Processing -> ProcessingRoute(
                host = processingHost,
                onOpenGuide = { showInterstitial { onOpenGuide(GUIDE_FROM_PROCESS) } },
                onPreviewMedia = onPreviewMedia,
            )

            MainTab.Downloaded -> LibraryRoute(
                isPrivate = false,
                trailingIconRes = R.drawable.ic_security,
                onTrailingAction = onOpenPrivateArea,
                onOpenMedia = onOpenMedia,
            )

            MainTab.Settings -> SettingsRoute(onOpenLanguage = onOpenLanguage)
        }
    }
}

const val GUIDE_FROM_HOME = "home"
const val GUIDE_FROM_PROCESS = "process"
