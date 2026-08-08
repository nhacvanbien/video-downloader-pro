package com.smarttool.videodownloader.feature.browser.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.browser.domain.usecase.GetBrowserSettingsUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.SetShowVideoAlertUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The two browser preferences the browser itself still reads: whether to alert on a
 * detected video, and whether to lock the page to portrait.
 *
 * Everything else this used to hold — thread counts, storage folder, desktop mode,
 * ad-blocker and dark-mode toggles — backed a settings screen that no longer exists;
 * the live settings screen is `feature/settings`. Those members were removed rather
 * than left as an API nothing calls.
 */
class BrowserSettingsViewModel(
    private val getBrowserSettings: GetBrowserSettingsUseCase,
    private val setShowVideoAlert: SetShowVideoAlertUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserSettingsContract.State())
    val uiState: StateFlow<BrowserSettingsContract.State> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = getBrowserSettings()
            _uiState.value = BrowserSettingsContract.State(
                lockPortrait = settings.lockPortrait,
                showVideoAlert = settings.showVideoAlert,
            )
        }
    }

    fun onEvent(event: BrowserSettingsContract.Event) {
        when (event) {
            BrowserSettingsContract.Event.DismissVideoAlert -> {
                _uiState.update { it.copy(showVideoAlert = false) }
                viewModelScope.launch { setShowVideoAlert(false) }
            }
        }
    }
}
