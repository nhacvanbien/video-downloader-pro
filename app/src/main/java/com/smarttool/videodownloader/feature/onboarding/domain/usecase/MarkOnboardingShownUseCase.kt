package com.smarttool.videodownloader.feature.onboarding.domain.usecase

import com.smarttool.videodownloader.feature.onboarding.domain.OnboardingRepository

class MarkOnboardingShownUseCase(private val repository: OnboardingRepository) {
    suspend operator fun invoke() = repository.markOnboardingShown()
}
