package com.smarttool.videodownloader.feature.splash.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.onboarding.domain.usecase.GetAppEntryPointUseCase
import com.smarttool.videodownloader.feature.onboarding.domain.usecase.IsStartLanguageShownUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Owns the one thing the splash reads from storage: how far through first-run the user
 * already is. The Activity itself only drives ads and the progress bar.
 */
class SplashViewModel(
    private val getAppEntryPoint: GetAppEntryPointUseCase,
    private val isStartLanguageShown: IsStartLanguageShownUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = SplashUiState(
                loaded = true,
                entryPoint = getAppEntryPoint(),
                startLanguageShown = isStartLanguageShown(),
            )
        }
    }

    /** Suspends until the flags are in, so callers never route on the defaults. */
    suspend fun awaitLoaded(): SplashUiState = uiState.first { it.loaded }
}
