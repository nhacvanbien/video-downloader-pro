package com.smarttool.videodownloader.feature.library.data

import android.content.Context
import androidx.lifecycle.asFlow
import com.smarttool.videodownloader.data.repository.VideoTaskItemRepository
import com.smarttool.videodownloader.feature.library.domain.model.LibraryQuery
import com.smarttool.videodownloader.feature.library.domain.model.MediaFilter
import com.smarttool.videodownloader.feature.library.domain.MediaLibraryRepository
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class MediaLibraryRepositoryImpl(
    private val videoTaskItemRepository: VideoTaskItemRepository,
    private val fileUtil: FileUtil,
) : MediaLibraryRepository {

    override fun observeLibrary(query: LibraryQuery): Flow<List<VideoTaskItem>> =
        videoTaskItemRepository.queryVideoTaskItem(
            query.filter == MediaFilter.All,
            query.filter.typeValue,
            query.search,
            query.sort.value,
        ).asFlow()

    override fun observePrivate(query: LibraryQuery): Flow<List<VideoTaskItem>> =
        videoTaskItemRepository.queryVideoTaskItemSecurity(
            query.filter == MediaFilter.All,
            query.filter.typeValue,
            query.search,
            query.sort.value,
        ).asFlow()

    override suspend fun delete(item: VideoTaskItem) =
        videoTaskItemRepository.deleteVideoTaskItem(item)

    override suspend fun setPrivate(id: String, isPrivate: Boolean) =
        videoTaskItemRepository.updateIsCheckSecurity(id, isPrivate)

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
