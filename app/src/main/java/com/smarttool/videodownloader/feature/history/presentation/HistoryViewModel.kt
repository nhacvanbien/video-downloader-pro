package com.smarttool.videodownloader.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    val uiState: StateFlow<HistoryContract.State> =
        combine(entries, searchQuery, isSearchActive) { entries, query, searchActive ->
            HistoryContract.State(
                mode = mode,
                entries = entries,
                searchQuery = query,
                isSearchActive = searchActive,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryContract.State(mode = mode),
        )

    fun onEvent(event: HistoryContract.Event) {
        when (event) {
            is HistoryContract.Event.SearchQueryChange -> searchQuery.value = event.query
            is HistoryContract.Event.SearchActiveChange -> {
                isSearchActive.value = event.active
                if (!event.active) searchQuery.value = ""
            }
            is HistoryContract.Event.DeleteEntry ->
                viewModelScope.launch { deleteEntry(event.entry) }
            is HistoryContract.Event.ClearHistory ->
                viewModelScope.launch { clearHistory() }
            is HistoryContract.Event.AddBookmark ->
                viewModelScope.launch { addBookmark(event.name, event.url) }
        }
    }
}
