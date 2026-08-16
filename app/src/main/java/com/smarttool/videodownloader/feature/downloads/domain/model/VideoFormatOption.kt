package com.smarttool.videodownloader.feature.downloads.domain.model

/**
 * A selectable download quality.
 *
 * @param label short form shown on the chip, e.g. "720P"
 * @param format the raw format string the downloader expects
 * @param url stream URL for this format
 * @param isAudio true for the audio-only pick — rendered as a distinct "Audio" chip
 */
data class VideoFormatOption(
    val label: String,
    val format: String,
    val url: String?,
    val isAudio: Boolean = false,
)
