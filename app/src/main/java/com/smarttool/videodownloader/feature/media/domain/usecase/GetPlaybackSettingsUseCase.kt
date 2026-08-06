package com.smarttool.videodownloader.feature.media.domain.usecase

import com.smarttool.videodownloader.feature.media.domain.MediaPreferencesRepository
import com.smarttool.videodownloader.feature.media.domain.model.PlaybackSettings

class GetPlaybackSettingsUseCase(private val repository: MediaPreferencesRepository) {
    operator fun invoke(): PlaybackSettings = PlaybackSettings(
        speed = repository.playbackSpeed(),
        looping = repository.isLooping(),
        fillMode = repository.isFillMode(),
    )
}
