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

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Re-read on resume: rating state changes after the rating dialog is completed. */
    fun refresh() {
        viewModelScope.launch {
            val settings = getSettings()
            _uiState.value = SettingsUiState(
                downloadLocation = settings.downloadLocation,
                showRateRow = !settings.isRated,
            )
        }
    }
}
