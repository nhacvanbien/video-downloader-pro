package com.smarttool.videodownloader.feature.downloads.domain.model

import com.smarttool.videodownloader.data.network.entity.VideoInfo

/** Preview/download actions raised from a browser tab. */
interface DownloadTabVideoListener {
    fun onPreviewVideo(videoInfo: VideoInfo, format: String, isForce: Boolean)

    fun onDownloadVideo(videoInfo: VideoInfo, format: String, videoTitle: String, isPrivate: Boolean = false)
}
