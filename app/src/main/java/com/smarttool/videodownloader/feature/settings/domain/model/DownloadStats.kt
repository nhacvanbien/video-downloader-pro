package com.smarttool.videodownloader.feature.settings.domain.model

data class DownloadStats(
    val videoCount: Int,
    val usedBytes: Long,
    val freeBytes: Long,
)
