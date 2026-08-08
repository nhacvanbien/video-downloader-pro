package com.smarttool.videodownloader.feature.splash.presentation

import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState
import com.smarttool.videodownloader.feature.onboarding.domain.model.AppEntryPoint

interface SplashContract {
    /**
     * @param loaded false until the onboarding flags have been read; the splash must not
     *   route or preload ads before then.
     */
    data class State(
        val loaded: Boolean = false,
        val entryPoint: AppEntryPoint = AppEntryPoint.Home,
        val startLanguageShown: Boolean = true,
    ) : UiState

    /** Nothing dispatches into the splash — it has no UI-triggered actions. */
    sealed interface Event : UiEvent
}
