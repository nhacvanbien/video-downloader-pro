package com.smarttool.videodownloader.feature.downloads.data

import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.data.repository.ProgressRepository
import com.smarttool.videodownloader.feature.downloads.domain.DownloadsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private const val POLL_INTERVAL_MILLIS = 1000L

class DownloadsRepositoryImpl(
    private val progressRepository: ProgressRepository,
    private val fileUtil: FileUtil,
) : DownloadsRepository {

    /**
     * The engines write progress into the store rather than emitting it, so the list
     * is polled. Finished rows are deleted as they are seen — leaving them behind
     * makes their IDs collide with new regular downloads and their progress stops
     * rendering.
     */
    override fun observeActiveDownloads(): Flow<List<ProgressInfo>> = flow {
        while (true) {
            val all = progressRepository.getProgressInfos().first()

            all.filter { it.downloadStatus == VideoTaskState.SUCCESS }
                .forEach { progressRepository.deleteProgressInfo(it) }

            emit(all.filter { it.downloadStatus != VideoTaskState.SUCCESS }.sortedBy { it.id })

            delay(POLL_INTERVAL_MILLIS)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun save(progressInfo: ProgressInfo) = withContext(Dispatchers.IO) {
        progressRepository.saveProgressInfo(progressInfo)
    }

    override suspend fun delete(progressInfo: ProgressInfo) = withContext(Dispatchers.IO) {
        progressRepository.deleteProgressInfo(progressInfo)
    }

    override fun ensureDownloadFolder(): Boolean =
        fileUtil.folderDir.exists() || fileUtil.folderDir.mkdirs()
}
