package com.smarttool.videodownloader.feature.browser.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.smarttool.videodownloader.VideoDownloaderApplication
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideosSheetHost

/**
 * The browser destination.
 *
 * The controller — and with it the WebView — is owned by the Activity, not by this
 * composable, so pushing a video preview on top and coming back keeps the page. Leaving
 * the browser is therefore an explicit [onBack]/[BackHandler] call rather than
 * something inferred from disposal.
 */
@Composable
fun WebTabRoute(
    controller: WebTabController,
    url: String,
    onOpenTabs: () -> Unit,
    onPreviewMedia: (url: String, title: String, headers: String) -> Unit,
    onBack: () -> Unit,
) {
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(url) {
        controller.onOpenTabs = onOpenTabs
        controller.onPreviewMedia = onPreviewMedia
        controller.onCloseBrowser = { currentOnBack() }
        controller.start(url)
    }

    // App-resume ads used to be suppressed by listing WebTabActivity in the ad SDK's
    // `listClassInValid`. With no Activity to name, the browser destination has to
    // suppress them itself — otherwise an app-open ad covers the page on every resume.
    DisposableEffect(Unit) {
        val appResumeAdHelper = VideoDownloaderApplication.instance.appResumeAdHelper
        appResumeAdHelper.setDisableAppResumeOnScreen()
        onDispose { appResumeAdHelper.setEnableAppResumeOnScreen() }
    }

    BackHandler { controller.closeTab() }

    WebTabScreen(
        state = controller.uiState,
        webView = controller.webViewContainer,
        fullscreenContainer = controller.fullscreenContainer,
        bannerAd = controller.bannerView,
        onUrlChange = controller::onUrlChange,
        onSubmitUrl = controller::submitUrl,
        onBack = controller::closeTab,
        onNavigateBack = controller::navigateBack,
        onNavigateForward = controller::navigateForward,
        onReload = controller::reloadPage,
        onShare = controller::share,
        onBookmark = controller::saveUrlToHistoryBookmark,
        onOpenTabs = controller::openTabs,
        onDownload = controller::requestDetectedVideos,
    )

    DetectedVideosSheetHost(presenter = controller.detected)
}
