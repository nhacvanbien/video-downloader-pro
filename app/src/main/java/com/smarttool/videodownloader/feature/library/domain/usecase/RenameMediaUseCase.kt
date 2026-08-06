package com.smarttool.videodownloader.feature.library.domain.usecase

import android.content.Context
import com.smarttool.videodownloader.feature.library.domain.MediaLibraryRepository
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem

/** Renames the file on disk and the library row, picking the right media type. */
class RenameMediaUseCase(private val repository: MediaLibraryRepository) {
    suspend operator fun invoke(
        context: Context,
        item: VideoTaskItem,
        newName: String,
        isImage: Boolean,
    ): Boolean = if (isImage) {
        repository.renameImage(context, item.mId, item.filePath, newName)
    } else {
        repository.renameVideo(context, item.mId, item.filePath, newName)
    }
}
