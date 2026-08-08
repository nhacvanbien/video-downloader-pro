package com.smarttool.videodownloader.feature.browser.presentation

import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState

interface BrowserSettingsContract {
    data class State(
        val lockPortrait: Boolean = false,
        val showVideoAlert: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data object DismissVideoAlert : Event
    }
}
