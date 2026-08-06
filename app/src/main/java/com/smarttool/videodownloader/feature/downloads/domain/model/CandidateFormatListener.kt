package com.smarttool.videodownloader.feature.downloads.domain.model

import com.smarttool.videodownloader.data.network.entity.VideoInfo

/** Notified when the user picks a download quality. */
interface CandidateFormatListener {
    fun onSelectFormat(videoInfo: VideoInfo, format: String)
}
