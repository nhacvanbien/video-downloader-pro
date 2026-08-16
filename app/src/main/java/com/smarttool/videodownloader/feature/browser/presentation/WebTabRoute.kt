package com.smarttool.videodownloader.feature.browser.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideosSheetHost
import com.smarttool.videodownloader.feature.downloads.presentation.NoMediaFoundDialog
import com.smarttool.videodownloader.feature.downloads.presentation.toUiState

/**
 * The browser destination.
 *
 * The host — and with it the WebView — is owned by the Activity, not by this
 * composable, so switching tabs or pushing a video preview on top and coming back both
 * keep the page. A session is already running (`host.started`) by the time this is
 * composed — starting one is the opener's job (see `AppNavHost.openWebTab`) — so
 * `host.tabViewModel`/`host.detector` are guaranteed initialized here. Leaving the
 * browser is therefore an explicit [onBack]/[BackHandler] call rather than something
 * inferred from disposal.
 */
@Composable
fun WebTabRoute(
    host: WebTabViewHost,
    onOpenTabs: () -> Unit,
    onPreviewMedia: (url: String, title: String, headers: String) -> Unit,
    onBack: () -> Unit,
) {
    host.onPreviewMedia = onPreviewMedia

    val tabViewModel = host.tabViewModel
    val detector = host.detector

    fun closeTab() {
        detector.onEvent(DetectedVideosContract.Event.CancelAllChecks)
        onBack()
    }

    // Mirrors Chrome: back while the omnibox is focused dismisses editing first, the
    // same press only leaves the tab once the suggestions overlay is already gone.
    BackHandler {
        if (tabViewModel.uiState.value.isTabInputFocused) {
            tabViewModel.onEvent(WebTabPipelineContract.Event.ChangeTabFocus(false))
        } else {
            closeTab()
        }
    }

    val tabState by tabViewModel.uiState.collectAsStateWithLifecycle()
    val detectorState by detector.uiState.collectAsStateWithLifecycle()

    val state = WebTabUiState(
        url = tabState.tabUrl,
        progress = tabState.progress,
        showProgress = tabState.progress != 100,
        canGoBack = tabState.canGoBack,
        canGoForward = tabState.canGoForward,
        isLoadingPage = tabState.isShowProgress,
        tabCount = tabState.tabCount,
        downloadButtonState = detectorState.downloadButtonState.toUiState(),
        isFullscreen = tabState.isFullscreen,
        isUrlFocused = tabState.isTabInputFocused,
        suggestions = tabState.suggestions,
    )

    fun submitUrl(input: String) {
        if (input.isNotEmpty()) {
            detector.onEvent(DetectedVideosContract.Event.CancelAllChecks)
            tabViewModel.onEvent(WebTabPipelineContract.Event.LoadPage(input))
        }
    }

    WebTabScreen(
        state = state,
        webView = host.webViewContainer,
        fullscreenContainer = host.fullscreenContainer,
        onUrlChange = { tabViewModel.onEvent(WebTabPipelineContract.Event.UrlChange(it)) },
        onUrlFocusChange = {
            tabViewModel.onEvent(WebTabPipelineContract.Event.ChangeTabFocus(it))
        },
        onSubmitUrl = { submitUrl(tabViewModel.uiState.value.tabUrl) },
        onSuggestionClick = { submitUrl(it) },
        onBack = ::closeTab,
        onReload = host::reloadPage,
        onOpenTabs = onOpenTabs,
        onDownload = { detector.onEvent(DetectedVideosContract.Event.ShowVideoInfo) },
    )

    DetectedVideosSheetHost(presenter = host.detected)

    if (host.showNoMediaFound) {
        NoMediaFoundDialog(
            onDismiss = { host.dismissNoMediaFound() },
            onReportIssue = { host.dismissNoMediaFound() },
        )
    }
}
