package com.smarttool.videodownloader.feature.library.domain.usecase

import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.feature.library.domain.MediaLibraryRepository
import com.smarttool.videodownloader.feature.library.domain.model.LibraryQuery
import kotlinx.coroutines.flow.Flow

class ObservePrivateLibraryUseCase(private val repository: MediaLibraryRepository) {
    operator fun invoke(query: LibraryQuery): Flow<List<VideoTaskItem>> =
        repository.observePrivate(query)
}
