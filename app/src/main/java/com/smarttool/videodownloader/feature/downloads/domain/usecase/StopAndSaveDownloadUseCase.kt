package com.smarttool.videodownloader.feature.downloads.domain.usecase

import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.downloads.domain.DownloaderGateway

class StopAndSaveDownloadUseCase(private val gateway: DownloaderGateway) {
    operator fun invoke(progressInfo: ProgressInfo) = gateway.stopAndSave(progressInfo)
}
