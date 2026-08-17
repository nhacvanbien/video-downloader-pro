package com.smarttool.videodownloader.feature.downloads.data

import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.data.repository.ProgressRepository
import com.smarttool.videodownloader.feature.downloads.domain.DownloadsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DownloadsRepositoryImpl(
    private val progressRepository: ProgressRepository,
    private val fileUtil: FileUtil,
) : DownloadsRepository {

    /**
     * Reacts directly to Room's Flow, which already emits on every write the engines
     * make (see ProgressDao). Polling this on a separate timer used to drift out of
     * phase with the workers' own ~1s save cadence, so the UI saw updates in uneven
     * bursts instead of one per write. Finished rows are deleted as they are seen —
     * leaving them behind makes their IDs collide with new regular downloads and their
     * progress stops rendering.
     */
    override fun observeActiveDownloads(): Flow<List<ProgressInfo>> =
        progressRepository.getProgressInfos().map { all ->
            all.filter { it.downloadStatus == VideoTaskState.SUCCESS }
                .forEach { progressRepository.deleteProgressInfo(it) }

            all.filter { it.downloadStatus != VideoTaskState.SUCCESS }.sortedBy { it.id }
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
