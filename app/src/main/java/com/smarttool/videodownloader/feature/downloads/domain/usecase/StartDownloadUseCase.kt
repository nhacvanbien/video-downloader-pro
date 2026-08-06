package com.smarttool.videodownloader.feature.downloads.domain.usecase

import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.feature.downloads.domain.DownloaderGateway
import com.smarttool.videodownloader.feature.downloads.domain.DownloadsRepository

class StartDownloadUseCase(
    private val repository: DownloadsRepository,
    private val gateway: DownloaderGateway,
) {
    suspend operator fun invoke(videoInfo: VideoInfo) {
        if (!repository.ensureDownloadFolder()) return

        val progressInfo = ProgressInfo(
            id = videoInfo.id,
            downloadId = videoInfo.id.hashCode().toLong(),
            videoInfo = videoInfo,
            isM3u8 = videoInfo.isM3u8,
        )

        repository.save(progressInfo)
        gateway.start(videoInfo)
    }
}
