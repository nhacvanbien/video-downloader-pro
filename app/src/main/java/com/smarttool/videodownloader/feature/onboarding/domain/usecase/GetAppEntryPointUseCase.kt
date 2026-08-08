package com.smarttool.videodownloader.feature.onboarding.domain.usecase

import com.smarttool.videodownloader.feature.onboarding.domain.OnboardingRepository
import com.smarttool.videodownloader.feature.onboarding.domain.model.AppEntryPoint

/** First run walks language → intro → home; each step is skipped once it is done. */
class GetAppEntryPointUseCase(private val repository: OnboardingRepository) {
    suspend operator fun invoke(): AppEntryPoint = when {
        !repository.isStartLanguageShown() -> AppEntryPoint.Language
        !repository.isOnboardingShown() -> AppEntryPoint.Intro
        else -> AppEntryPoint.Home
    }
}
