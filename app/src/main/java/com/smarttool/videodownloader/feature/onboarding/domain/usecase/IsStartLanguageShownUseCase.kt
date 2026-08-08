package com.smarttool.videodownloader.feature.onboarding.domain.usecase

import com.smarttool.videodownloader.feature.onboarding.domain.OnboardingRepository

class IsStartLanguageShownUseCase(private val repository: OnboardingRepository) {
    suspend operator fun invoke(): Boolean = repository.isStartLanguageShown()
}
