package com.smarttool.videodownloader.feature.downloads.domain.usecase

import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.downloads.domain.DownloaderGateway
import com.smarttool.videodownloader.feature.downloads.domain.DownloadsRepository

class CancelDownloadUseCase(
    private val repository: DownloadsRepository,
    private val gateway: DownloaderGateway,
) {
    suspend operator fun invoke(progressInfo: ProgressInfo, removeFile: Boolean) {
        repository.delete(progressInfo)
        gateway.cancel(progressInfo, removeFile)
    }
}
