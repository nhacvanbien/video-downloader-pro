package com.smarttool.videodownloader.feature.downloads.domain.usecase

import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.feature.downloads.domain.DownloadsRepository
import kotlinx.coroutines.flow.first

/**
 * Re-attempts every download parked in [VideoTaskState.WAITING_FOR_WIFI] — called once Wi-Fi
 * comes back or "Wi-Fi Only" is turned off. [StartDownloadUseCase] re-checks the gate itself,
 * so an item that is still blocked is simply re-saved as waiting.
 */
class RetryWaitingForWifiDownloadsUseCase(
    private val repository: DownloadsRepository,
    private val startDownload: StartDownloadUseCase,
) {
    suspend operator fun invoke() {
        repository.observeActiveDownloads().first()
            .filter { it.downloadStatus == VideoTaskState.WAITING_FOR_WIFI }
            .forEach { startDownload(it.videoInfo) }
    }
}
