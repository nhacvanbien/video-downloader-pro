package com.smarttool.videodownloader.feature.splash.presentation

import com.smarttool.videodownloader.feature.onboarding.domain.model.AppEntryPoint

/**
 * @param loaded false until the onboarding flags have been read; the splash must not
 *   route or preload ads before then.
 */
data class SplashUiState(
    val loaded: Boolean = false,
    val entryPoint: AppEntryPoint = AppEntryPoint.Home,
    val startLanguageShown: Boolean = true,
)
