package com.smarttool.videodownloader.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry
import com.smarttool.videodownloader.feature.history.domain.usecase.AddBookmarkUseCase
import com.smarttool.videodownloader.feature.history.domain.usecase.ClearHistoryUseCase
import com.smarttool.videodownloader.feature.history.domain.usecase.DeleteHistoryEntryUseCase
import com.smarttool.videodownloader.feature.history.domain.usecase.ObserveBookmarksUseCase
import com.smarttool.videodownloader.feature.history.domain.usecase.ObserveHistoryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryMode { HISTORY, BOOKMARK }

data class HistoryUiState(
    val mode: HistoryMode = HistoryMode.HISTORY,
    val entries: List<HistoryEntry> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val mode: HistoryMode,
    observeHistory: ObserveHistoryUseCase,
    observeBookmarks: ObserveBookmarksUseCase,
    private val addBookmark: AddBookmarkUseCase,
    private val deleteEntry: DeleteHistoryEntryUseCase,
    private val clearHistory: ClearHistoryUseCase,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isSearchActive = MutableStateFlow(false)

    private val entries = searchQuery.flatMapLatest { query ->
        when (mode) {
            HistoryMode.HISTORY -> observeHistory(query)
            HistoryMode.BOOKMARK -> observeBookmarks(query)
        }
    }

    val uiState: StateFlow<HistoryUiState> =
        combine(entries, searchQuery, isSearchActive) { entries, query, searchActive ->
            HistoryUiState(
                mode = mode,
                entries = entries,
                searchQuery = query,
                isSearchActive = searchActive,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(mode = mode),
        )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onSearchActiveChange(active: Boolean) {
        isSearchActive.value = active
        if (!active) searchQuery.value = ""
    }

    fun onDeleteEntry(entry: HistoryEntry) {
        viewModelScope.launch { deleteEntry(entry) }
    }

    fun onClearHistory() {
        viewModelScope.launch { clearHistory() }
    }

    fun onAddBookmark(name: String, url: String) {
        viewModelScope.launch { addBookmark(name, url) }
    }
}
