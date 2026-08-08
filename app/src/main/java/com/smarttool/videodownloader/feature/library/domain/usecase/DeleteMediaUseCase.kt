package com.smarttool.videodownloader.feature.library.domain.usecase

import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.feature.library.domain.MediaLibraryRepository

class DeleteMediaUseCase(private val repository: MediaLibraryRepository) {
    suspend operator fun invoke(item: VideoTaskItem) = repository.delete(item)
}
