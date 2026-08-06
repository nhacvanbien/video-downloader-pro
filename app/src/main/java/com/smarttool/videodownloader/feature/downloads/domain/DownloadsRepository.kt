package com.smarttool.videodownloader.feature.downloads.domain

import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import kotlinx.coroutines.flow.Flow

interface DownloadsRepository {
    /** In-flight downloads, polled from the progress store; completed rows are pruned. */
    fun observeActiveDownloads(): Flow<List<ProgressInfo>>

    suspend fun save(progressInfo: ProgressInfo)

    suspend fun delete(progressInfo: ProgressInfo)

    /** True when the download folder exists or could be created. */
    fun ensureDownloadFolder(): Boolean
}
