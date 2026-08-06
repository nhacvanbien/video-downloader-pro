package com.smarttool.videodownloader.feature.settings.domain.usecase

import com.smarttool.videodownloader.feature.settings.domain.SettingsRepository
import com.smarttool.videodownloader.feature.settings.domain.model.SettingsInfo

class GetSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): SettingsInfo = SettingsInfo(
        downloadLocation = repository.downloadLocation(),
        isRated = repository.isRated(),
    )
}
