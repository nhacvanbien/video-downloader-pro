package com.smarttool.videodownloader.feature.library.domain.usecase

import android.content.Context
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.feature.library.domain.MediaLibraryRepository

class SetMediaPrivateUseCase(private val repository: MediaLibraryRepository) {
    suspend operator fun invoke(
        context: Context,
        item: VideoTaskItem,
        isPrivate: Boolean,
        onProgress: (Float) -> Unit = {},
    ): Boolean = repository.setPrivate(context, item, isPrivate, onProgress)
}
