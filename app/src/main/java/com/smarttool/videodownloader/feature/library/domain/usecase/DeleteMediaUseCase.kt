package com.smarttool.videodownloader.feature.library.domain.usecase

import com.smarttool.videodownloader.feature.library.domain.MediaLibraryRepository
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem

class DeleteMediaUseCase(private val repository: MediaLibraryRepository) {
    suspend operator fun invoke(item: VideoTaskItem) = repository.delete(item)
}
