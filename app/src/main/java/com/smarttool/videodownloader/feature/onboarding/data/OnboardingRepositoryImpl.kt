package com.smarttool.videodownloader.feature.onboarding.data

import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.feature.onboarding.domain.OnboardingRepository
import kotlinx.coroutines.flow.first

class OnboardingRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
) : OnboardingRepository {

    override suspend fun isStartLanguageShown(): Boolean =
        preferences.showedStartLanguage.first()

    override suspend fun isOnboardingShown(): Boolean = preferences.showedOnboarding.first()

    override suspend fun markOnboardingShown() = preferences.setShowedOnboarding(true)

    override suspend fun shouldShowPermissionStep(): Boolean =
        preferences.showPermissionStep.first()

    override suspend fun markPermissionStepDone() = preferences.setShowPermissionStep(false)
}
