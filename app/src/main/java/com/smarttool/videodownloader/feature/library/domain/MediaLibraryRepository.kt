package com.smarttool.videodownloader.feature.library.domain

import android.content.Context
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.feature.library.domain.model.LibraryQuery
import kotlinx.coroutines.flow.Flow

interface MediaLibraryRepository {
    /** Items visible in the normal library (i.e. not moved to the private area). */
    fun observeLibrary(query: LibraryQuery): Flow<List<VideoTaskItem>>

    /** Items the user has moved behind the PIN. */
    fun observePrivate(query: LibraryQuery): Flow<List<VideoTaskItem>>

    suspend fun delete(item: VideoTaskItem)

    suspend fun setPrivate(id: String, isPrivate: Boolean)

    suspend fun renameVideo(context: Context, id: String, filePath: String, newName: String): Boolean

    suspend fun renameImage(context: Context, id: String, filePath: String, newName: String): Boolean

    /** Drops rows whose backing file has been removed outside the app. */
    suspend fun pruneMissingFiles()
}
