package com.smarttool.videodownloader.feature.browser.domain.usecase

import com.smarttool.videodownloader.feature.browser.domain.BrowserPreferencesRepository

class GetVideoDetectionThresholdUseCase(private val repository: BrowserPreferencesRepository) {
    suspend operator fun invoke(): Int = repository.videoDetectionThreshold()
}
