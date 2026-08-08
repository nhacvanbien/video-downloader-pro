package com.smarttool.videodownloader.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.settings.domain.usecase.GetSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getSettings: GetSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsContract.State())
    val uiState: StateFlow<SettingsContract.State> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onEvent(event: SettingsContract.Event) {
        when (event) {
            is SettingsContract.Event.Refresh -> refresh()
        }
    }

    /** Re-read on resume: rating state changes after the rating dialog is completed. */
    private fun refresh() {
        viewModelScope.launch {
            val settings = getSettings()
            _uiState.value = SettingsContract.State(
                downloadLocation = settings.downloadLocation,
                showRateRow = !settings.isRated,
            )
        }
    }
}
