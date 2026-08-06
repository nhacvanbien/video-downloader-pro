package com.smarttool.videodownloader.feature.downloads.presentation

data class ProcessingUiState(
    val url: String = "",
    val downloadButtonState: DownloadButtonUiState = DownloadButtonUiState.Disabled,
)
