package com.smarttool.videodownloader.feature.downloads.presentation

import com.smarttool.videodownloader.feature.downloads.domain.model.VideoFormatOption

/** One detected video as the picker sheet renders it. */
data class DetectedVideoUi(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    /** e.g. "M3U8 List", "Regular MP4 Download"; empty when unknown. */
    val typeLabel: String,
    /** Only known ahead of time for regular downloads. */
    val readableSize: String?,
    val formats: List<VideoFormatOption>,
    val selectedFormat: String?,
)
