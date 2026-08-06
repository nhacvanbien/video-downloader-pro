package com.smarttool.videodownloader.feature.library.domain.usecase

import com.smarttool.videodownloader.feature.library.domain.MediaLibraryRepository

class SetMediaPrivateUseCase(private val repository: MediaLibraryRepository) {
    suspend operator fun invoke(id: String, isPrivate: Boolean) =
        repository.setPrivate(id, isPrivate)
}
