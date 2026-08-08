package com.smarttool.videodownloader.feature.downloads.presentation

/** Rendering shape `ProcessingRoute` builds by combining `ProcessingViewModel`'s and
 * `DetectedVideosTabViewModel`'s state — not owned/mutated by either ViewModel itself. */
data class ProcessingUiState(
    val url: String = "",
    val downloadButtonState: DownloadButtonUiState = DownloadButtonUiState.Disabled,
)
