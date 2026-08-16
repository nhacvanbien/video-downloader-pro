package com.smarttool.videodownloader.feature.settings.presentation

import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState
import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine

interface SettingsContract {
    data class State(
        val downloadLocation: String = "",
        val downloadLocationSubfolder: String = "",
        val showRateRow: Boolean = false,
        val wifiOnly: Boolean = false,
        val searchEngine: SearchEngine = SearchEngine.GOOGLE,
        val searchEngineSheetVisible: Boolean = false,
        val downloadLocationEditorVisible: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object Refresh : Event
        data class SetWifiOnly(val enabled: Boolean) : Event
        data class SetSearchEngine(val engine: SearchEngine) : Event
        data class SetSearchEngineSheetVisible(val visible: Boolean) : Event
        data class SetDownloadLocationEditorVisible(val visible: Boolean) : Event
        data class SetDownloadLocationSubfolder(val name: String) : Event
    }
}
