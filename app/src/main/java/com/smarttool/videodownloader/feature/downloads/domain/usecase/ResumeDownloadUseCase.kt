package com.smarttool.videodownloader.feature.downloads.domain.usecase

import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.downloads.domain.DownloaderGateway
import com.smarttool.videodownloader.feature.downloads.domain.DownloadsRepository

class ResumeDownloadUseCase(
    private val repository: DownloadsRepository,
    private val gateway: DownloaderGateway,
) {
    suspend operator fun invoke(progressInfo: ProgressInfo) {
        if (progressInfo.videoInfo.isRegularDownload) {
            gateway.resume(progressInfo)
            return
        }

        val updated = progressInfo.copy(downloadStatus = VideoTaskState.PREPARE)
        repository.save(updated)
        gateway.resume(updated)
    }
}
