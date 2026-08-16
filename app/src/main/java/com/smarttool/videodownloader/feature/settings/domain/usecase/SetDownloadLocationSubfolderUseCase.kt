package com.smarttool.videodownloader.feature.settings.domain.usecase

import com.smarttool.videodownloader.feature.settings.domain.SettingsRepository

class SetDownloadLocationSubfolderUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(name: String) = repository.setDownloadLocationSubfolder(name)
}
