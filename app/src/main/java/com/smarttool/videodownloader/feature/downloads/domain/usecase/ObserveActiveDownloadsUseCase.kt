package com.smarttool.videodownloader.feature.downloads.domain.usecase

import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.downloads.domain.DownloadsRepository
import kotlinx.coroutines.flow.Flow

class ObserveActiveDownloadsUseCase(private val repository: DownloadsRepository) {
    operator fun invoke(): Flow<List<ProgressInfo>> = repository.observeActiveDownloads()
}
