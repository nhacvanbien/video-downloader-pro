package com.smarttool.videodownloader.feature.onboarding.domain.usecase

import com.smarttool.videodownloader.feature.onboarding.domain.OnboardingRepository

class CompletePermissionStepUseCase(private val repository: OnboardingRepository) {
    suspend operator fun invoke() = repository.markPermissionStepDone()
}
