package com.smarttool.videodownloader.feature.browser.presentation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideosSheetHost
import com.smarttool.videodownloader.feature.downloads.presentation.toUiState

/**
 * The browser destination.
 *
 * The host — and with it the WebView — is owned by the Activity, not by this
 * composable, so pushing a video preview on top and coming back keeps the page. Leaving
 * the browser is therefore an explicit [onBack]/[BackHandler] call rather than
 * something inferred from disposal.
 */
@Composable
fun WebTabRoute(
    host: WebTabViewHost,
    url: String,
    onOpenTabs: () -> Unit,
    onPreviewMedia: (url: String, title: String, headers: String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    // Runs synchronously during composition (unlike LaunchedEffect, whose body only
    // fires after the first frame) so `host.tabViewModel`/`host.detector` below are
    // guaranteed to be initialized before they're read.
    remember(url) {
        host.onPreviewMedia = onPreviewMedia
        host.start(url)
    }

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
        onNavigateBack = host::navigateBack,
        onNavigateForward = host::navigateForward,
        onReload = host::reloadPage,
        onShare = {
            val current = tabViewModel.uiState.value
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, current.currentTitle)
                putExtra(Intent.EXTRA_TEXT, current.tabUrl)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.string_share)))
        },
        onBookmark = { tabViewModel.onEvent(WebTabPipelineContract.Event.SaveUrlToHistoryBookmark) },
        onOpenTabs = onOpenTabs,
        onDownload = { detector.onEvent(DetectedVideosContract.Event.ShowVideoInfo) },
    )

    DetectedVideosSheetHost(presenter = host.detected)
}
