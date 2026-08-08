package com.smarttool.videodownloader.feature.browser.domain.usecase

import com.smarttool.videodownloader.feature.browser.domain.BrowserPreferencesRepository

class SetShowVideoAlertUseCase(private val repository: BrowserPreferencesRepository) {
    suspend operator fun invoke(show: Boolean) = repository.setShowVideoAlert(show)
}
