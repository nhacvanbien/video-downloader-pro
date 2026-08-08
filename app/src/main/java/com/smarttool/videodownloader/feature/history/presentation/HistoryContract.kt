package com.smarttool.videodownloader.feature.history.presentation

import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState
import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry

interface HistoryContract {
    data class State(
        val mode: HistoryMode = HistoryMode.HISTORY,
        val entries: List<HistoryEntry> = emptyList(),
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class SearchQueryChange(val query: String) : Event
        data class SearchActiveChange(val active: Boolean) : Event
        data class DeleteEntry(val entry: HistoryEntry) : Event
        data object ClearHistory : Event
        data class AddBookmark(val name: String, val url: String) : Event
    }
}
