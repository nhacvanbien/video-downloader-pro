package com.smarttool.videodownloader.feature.browser.presentation

import com.smarttool.videodownloader.feature.downloads.presentation.DownloadButtonUiState

data class WebTabUiState(
    val url: String = "",
    val progress: Int = 0,
    val showProgress: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    /** While true the reload control acts as stop-loading. */
    val isLoadingPage: Boolean = false,
    val tabCount: Int = 0,
    val downloadButtonState: DownloadButtonUiState = DownloadButtonUiState.Disabled,
    /** True while a page element is playing fullscreen; the browser chrome hides. */
    val isFullscreen: Boolean = false,
)
