package com.smarttool.videodownloader.feature.browser.presentation

import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel

interface BrowserHomeContract {
    data class State(
        val query: String = "",
        val tabCount: Int = 0,
    ) : UiState

    sealed interface Event : UiEvent {
        data class QueryChange(val query: String) : Event
        data object ClearQuery : Event
        data class OpenTab(val tab: TabModel) : Event
    }

    sealed interface Effect : UiEffect {
        /** Tab was persisted; the host should now open the web view on it. */
        data class TabReady(val tab: TabModel) : Effect
    }
}
