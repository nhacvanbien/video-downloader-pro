package com.smarttool.videodownloader.feature.media.domain.usecase

import com.smarttool.videodownloader.feature.media.domain.MediaPreferencesRepository

class SetLoopingUseCase(private val repository: MediaPreferencesRepository) {
    suspend operator fun invoke(looping: Boolean) = repository.setLooping(looping)
}
