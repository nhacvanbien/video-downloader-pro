package com.smarttool.videodownloader.feature.settings.domain.usecase

import com.smarttool.videodownloader.feature.settings.domain.SettingsRepository
import com.smarttool.videodownloader.feature.settings.domain.model.DownloadStats

class GetDownloadStatsUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(): DownloadStats = repository.getDownloadStats()
}
