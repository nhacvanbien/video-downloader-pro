package com.smarttool.videodownloader.feature.library.data

import android.content.Context
import androidx.lifecycle.asFlow
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.data.repository.VideoTaskItemRepository
import com.smarttool.videodownloader.feature.library.domain.MediaLibraryRepository
import com.smarttool.videodownloader.feature.library.domain.model.LibraryQuery
import com.smarttool.videodownloader.feature.library.domain.model.MediaFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class MediaLibraryRepositoryImpl(
    private val videoTaskItemRepository: VideoTaskItemRepository,
    private val fileUtil: FileUtil,
) : MediaLibraryRepository {

    // The type predicate is applied in Kotlin, not in SQL: `mime_type` says "video" for audio
    // files written before the column started distinguishing them, so a SQL `mime_type = 'audio'`
    // left those rows stuck under the Video chip while the list row itself already rendered them
    // as audio. [MediaFilter.forFile] is the same classifier the rows use. Search and sort stay
    // in SQL.
    override fun observeLibrary(query: LibraryQuery): Flow<List<VideoTaskItem>> =
        videoTaskItemRepository.queryVideoTaskItem(
            true,
            MediaFilter.All.typeValue,
            query.search,
            query.sort.value,
        ).asFlow().map { it.matching(query.filter) }

    override fun observePrivate(query: LibraryQuery): Flow<List<VideoTaskItem>> =
        videoTaskItemRepository.queryVideoTaskItemSecurity(
            true,
            MediaFilter.All.typeValue,
            query.search,
            query.sort.value,
        ).asFlow().map { it.matching(query.filter) }

    private fun List<VideoTaskItem>.matching(target: MediaFilter): List<VideoTaskItem> =
        if (target == MediaFilter.All) {
            this
        } else {
            filter { MediaFilter.forFile(it.mimeType, it.fileName) == target }
        }

    override suspend fun delete(item: VideoTaskItem) =
        videoTaskItemRepository.deleteVideoTaskItem(item)

    override suspend fun setPrivate(
        context: Context,
        item: VideoTaskItem,
        isPrivate: Boolean,
        onProgress: (Float) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        val newPath = if (isPrivate) {
            fileUtil.moveToPrivateStorage(context, item.filePath, onProgress)
        } else {
            fileUtil.moveOutOfPrivateStorage(
                context,
                item.filePath,
                item.mimeType.startsWith("image"),
                onProgress,
            )
        } ?: return@withContext false

        videoTaskItemRepository.updateSecurityAndPath(item.mId, isPrivate, newPath)
        true
    }

    override suspend fun renameVideo(
        context: Context,
        id: String,
        filePath: String,
        newName: String,
    ): Boolean {
        if (newName.isEmpty() || videoTaskItemRepository.isFileNameVideoExists(newName)) return false

        val renamed = fileUtil.renameMedia(context, filePath, newName) ?: return false
        videoTaskItemRepository.updateNameVideoTaskItem(id, renamed.first, renamed.second)
        return true
    }

    override suspend fun renameImage(
        context: Context,
        id: String,
        filePath: String,
        newName: String,
    ): Boolean {
        if (newName.isEmpty() || videoTaskItemRepository.isFileNameImageExists(newName)) return false

        val renamed = fileUtil.renameImage(context, filePath, newName) ?: return false
        videoTaskItemRepository.updateNameVideoTaskItem(id, renamed.first, renamed.second)
        return true
    }

    override suspend fun pruneMissingFiles() = withContext(Dispatchers.IO) {
        val missing = videoTaskItemRepository.getAllVideoTaskItems()
            .filter { !File(it.filePath).exists() }

        if (missing.isNotEmpty()) {
            videoTaskItemRepository.deleteVideoTaskItems(missing)
        }
    }
}
