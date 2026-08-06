package com.smarttool.videodownloader.feature.media.domain.usecase

import com.smarttool.videodownloader.feature.media.domain.MediaPreferencesRepository

class SetPlaybackSpeedUseCase(private val repository: MediaPreferencesRepository) {
    operator fun invoke(speed: Float) = repository.setPlaybackSpeed(speed)
}
