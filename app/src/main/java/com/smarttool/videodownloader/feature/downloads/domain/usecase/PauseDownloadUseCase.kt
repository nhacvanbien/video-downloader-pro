package com.smarttool.videodownloader.feature.downloads.domain.usecase

import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.downloads.domain.DownloaderGateway
import com.smarttool.videodownloader.feature.downloads.domain.DownloadsRepository
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState

class PauseDownloadUseCase(
    private val repository: DownloadsRepository,
    private val gateway: DownloaderGateway,
) {
    suspend operator fun invoke(progressInfo: ProgressInfo) {
        // Regular downloads are paused directly; engine-backed ones need their
        // persisted state updated first so progress resumes from the right point.
        if (progressInfo.videoInfo.isRegularDownload) {
            gateway.pause(progressInfo)
            return
        }

        val updated = progressInfo.copy(downloadStatus = VideoTaskState.PAUSE)
        repository.save(updated)
        gateway.pause(updated)
    }
}
