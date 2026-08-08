package com.smarttool.videodownloader.feature.downloads.presentation

import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonState
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateCanDownload
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateLoading

/** State of the download affordance beside the paste field. */
enum class DownloadButtonUiState {
    /** No usable link entered yet. */
    Disabled,

    /** The hidden WebView is probing the page for media. */
    Loading,

    /** At least one downloadable video was detected. */
    Enabled,
}

fun DownloadButtonState.toUiState(): DownloadButtonUiState = when (this) {
    is DownloadButtonStateCanDownload -> DownloadButtonUiState.Enabled
    is DownloadButtonStateLoading -> DownloadButtonUiState.Loading
    else -> DownloadButtonUiState.Disabled
}
