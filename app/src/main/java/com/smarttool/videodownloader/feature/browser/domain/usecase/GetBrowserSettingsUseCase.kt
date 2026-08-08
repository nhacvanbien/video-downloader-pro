package com.smarttool.videodownloader.feature.browser.domain.usecase

import com.smarttool.videodownloader.feature.browser.domain.BrowserPreferencesRepository
import com.smarttool.videodownloader.feature.browser.domain.model.BrowserSettings

class GetBrowserSettingsUseCase(private val repository: BrowserPreferencesRepository) {
    suspend operator fun invoke(): BrowserSettings = BrowserSettings(
        showVideoAlert = repository.isShowVideoAlert(),
        lockPortrait = repository.isLockPortrait(),
    )
}
