package com.smarttool.videodownloader.feature.media.domain.usecase

import com.smarttool.videodownloader.feature.media.domain.MediaPreferencesRepository

class SetFillModeUseCase(private val repository: MediaPreferencesRepository) {
    operator fun invoke(fill: Boolean) = repository.setFillMode(fill)
}
