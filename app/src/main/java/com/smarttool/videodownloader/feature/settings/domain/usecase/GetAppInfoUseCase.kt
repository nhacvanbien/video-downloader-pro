package com.smarttool.videodownloader.feature.settings.domain.usecase

import com.smarttool.videodownloader.feature.settings.domain.SettingsRepository
import com.smarttool.videodownloader.feature.settings.domain.model.AppInfo

class GetAppInfoUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): AppInfo = repository.appInfo()
}
