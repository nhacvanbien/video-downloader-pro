package com.smarttool.videodownloader.feature.settings.presentation

import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState

interface SettingsContract {
    data class State(
        val downloadLocation: String = "",
        val showRateRow: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object Refresh : Event
    }
}
