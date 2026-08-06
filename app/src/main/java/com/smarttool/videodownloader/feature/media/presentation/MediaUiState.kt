package com.smarttool.videodownloader.feature.media.presentation

data class MediaUiState(
    val title: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val looping: Boolean = false,
    val fillMode: Boolean = false,
    /** Only downloaded items expose the detail/more action. */
    val showMoreAction: Boolean = false,
)
