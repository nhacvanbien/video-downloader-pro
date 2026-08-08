package com.smarttool.videodownloader.feature.onboarding.domain

/**
 * The flags that record how far through first-run the user has got. Splash reads them
 * to pick a start destination; the intro and permission steps mark themselves done.
 */
interface OnboardingRepository {
    suspend fun isStartLanguageShown(): Boolean

    suspend fun isOnboardingShown(): Boolean

    suspend fun markOnboardingShown()

    /** False once the user has been through — or skipped — the permission step. */
    suspend fun shouldShowPermissionStep(): Boolean

    suspend fun markPermissionStepDone()
}
