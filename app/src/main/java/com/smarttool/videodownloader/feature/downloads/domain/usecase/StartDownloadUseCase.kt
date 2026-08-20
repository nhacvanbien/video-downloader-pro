package com.smarttool.videodownloader.feature.downloads.domain.usecase

import com.smarttool.videodownloader.core.network.NetworkMonitor
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.feature.downloads.domain.DownloaderGateway
import com.smarttool.videodownloader.feature.downloads.domain.DownloadsRepository

class StartDownloadUseCase(
    private val repository: DownloadsRepository,
    private val gateway: DownloaderGateway,
    private val getWifiOnly: GetWifiOnlyUseCase,
    private val networkMonitor: NetworkMonitor,
) {
    /**
     * @param ignoreWifiOnly starts the item even when the "Wi-Fi Only" gate would park it —
     * used by the "Download now" action on a row already waiting for Wi-Fi, where the user
     * has explicitly asked for this one download to go ahead on the current connection.
     * @return false when blocked by "Wi-Fi Only" and off Wi-Fi — the item is persisted as
     * [VideoTaskState.WAITING_FOR_WIFI] (so it shows up in the Downloads tab) instead of the
     * engine being started; nothing is enqueued in [gateway] until a retry succeeds.
     */
    suspend operator fun invoke(videoInfo: VideoInfo, ignoreWifiOnly: Boolean = false): Boolean {
        if (!ignoreWifiOnly && getWifiOnly() && !networkMonitor.isOnWifi()) {
            repository.save(
                ProgressInfo(
                    id = videoInfo.id,
                    downloadId = videoInfo.id.hashCode().toLong(),
                    videoInfo = videoInfo,
                    isM3u8 = videoInfo.isM3u8,
                    downloadStatus = VideoTaskState.WAITING_FOR_WIFI,
                ),
            )
            return false
        }
        if (!repository.ensureDownloadFolder()) return true

        val progressInfo = ProgressInfo(
            id = videoInfo.id,
            downloadId = videoInfo.id.hashCode().toLong(),
            videoInfo = videoInfo,
            isM3u8 = videoInfo.isM3u8,
        )

        repository.save(progressInfo)
        gateway.start(videoInfo)
        return true
    }
}
