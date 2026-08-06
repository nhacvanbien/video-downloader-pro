package com.smarttool.videodownloader.feature.downloads.presentation

/** State of the download affordance beside the paste field. */
enum class DownloadButtonUiState {
    /** No usable link entered yet. */
    Disabled,

    /** The hidden WebView is probing the page for media. */
    Loading,

    /** At least one downloadable video was detected. */
    Enabled,
}
