package com.smarttool.videodownloader.feature.library.domain.usecase

import com.smarttool.videodownloader.feature.library.domain.MediaLibraryRepository
import com.smarttool.videodownloader.feature.library.domain.model.LibraryQuery
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import kotlinx.coroutines.flow.Flow

class ObserveLibraryUseCase(private val repository: MediaLibraryRepository) {
    operator fun invoke(query: LibraryQuery): Flow<List<VideoTaskItem>> =
        repository.observeLibrary(query)
}
