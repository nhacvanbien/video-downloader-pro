package com.smarttool.videodownloader.feature.downloads.domain.model

interface DownloadTabListener : DownloadTabVideoListener, CandidateFormatListener {
    fun onCancel()
}
