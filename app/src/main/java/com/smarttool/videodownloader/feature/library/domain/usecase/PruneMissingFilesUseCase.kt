package com.smarttool.videodownloader.feature.library.domain.usecase

import com.smarttool.videodownloader.feature.library.domain.MediaLibraryRepository

class PruneMissingFilesUseCase(private val repository: MediaLibraryRepository) {
    suspend operator fun invoke() = repository.pruneMissingFiles()
}
