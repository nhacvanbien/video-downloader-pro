package com.smarttool.videodownloader.feature.library.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.feature.library.domain.model.MediaFilter
import com.smarttool.videodownloader.feature.library.domain.model.SortState
import com.smarttool.videodownloader.feature.library.domain.usecase.DeleteMediaUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.GetSortTypeUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.ObserveLibraryUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.ObservePrivateLibraryUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.RenameMediaUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.SetMediaPrivateUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.SetSortTypeUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs both the Downloaded tab and the private (PIN-protected) library — [isPrivate]
 * selects which slice is observed; every other behaviour is identical.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val isPrivate: Boolean,
    observeLibrary: ObserveLibraryUseCase,
    observePrivateLibrary: ObservePrivateLibraryUseCase,
    private val deleteMedia: DeleteMediaUseCase,
    private val renameMedia: RenameMediaUseCase,
    private val setMediaPrivate: SetMediaPrivateUseCase,
    private val getSortType: GetSortTypeUseCase,
    private val setSortType: SetSortTypeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryContract.State())
    val uiState: StateFlow<LibraryContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<LibraryContract.Effect>(Channel.BUFFERED)
    val effect: Flow<LibraryContract.Effect> = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            val sort = SortState.getSortState(getSortType())
            _uiState.update { it.copy(query = it.query.copy(sort = sort)) }
        }
    }

    // Keyed on `query` alone: flatMapLatest over the whole state would tear down and re-run the
    // Room query on every unrelated change, including each progress tick of a private move.
    val items: StateFlow<List<VideoTaskItem>> = _uiState
        .map { it.query }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (isPrivate) observePrivateLibrary(query) else observeLibrary(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The same slice as [items] with the type filter dropped, so the filter chips can each show
     * how many files they would reveal. Reading it off [items] instead would collapse every
     * chip's count onto the one currently selected. Search still applies — the counts describe
     * what the current search would yield, not the whole library.
     */
    val itemsIgnoringTypeFilter: StateFlow<List<VideoTaskItem>> = _uiState
        .map { it.query.copy(filter = MediaFilter.All) }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (isPrivate) observePrivateLibrary(query) else observeLibrary(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onEvent(event: LibraryContract.Event) {
        when (event) {
            is LibraryContract.Event.FilterChange -> onFilterChange(event.filter)
            is LibraryContract.Event.SearchChange -> onSearchChange(event.search)
            is LibraryContract.Event.SortChange -> onSortChange(event.sort)
            is LibraryContract.Event.SetSortSheetVisible -> setSortSheetVisible(event.visible)
            is LibraryContract.Event.SetViewMode -> _uiState.update { it.copy(viewMode = event.mode) }
            is LibraryContract.Event.SetSelectionMode -> setSelectionMode(event.enabled)
            is LibraryContract.Event.ToggleSelection -> toggleSelection(event.id)
            is LibraryContract.Event.SelectAll -> selectAll()
            is LibraryContract.Event.ClearSelection -> clearSelection()
            is LibraryContract.Event.DeleteSelected -> deleteSelected()
            is LibraryContract.Event.Delete -> delete(event.item)
            is LibraryContract.Event.Rename -> rename(event.context, event.item, event.newName)
            is LibraryContract.Event.MoveSelectedToPrivate ->
                moveSelectedToPrivate(event.context, event.makePrivate)
        }
    }

    private fun onFilterChange(filter: MediaFilter) {
        // selectionMode and selectedIds are intentionally left untouched: item ids are
        // stable across filters, so a selection made under one chip should still show
        // selected after switching to another (and back), not silently reset.
        _uiState.update { it.copy(query = it.query.copy(filter = filter)) }
    }

    private fun onSearchChange(search: String) {
        _uiState.update { it.copy(query = it.query.copy(search = search)) }
    }

    private fun onSortChange(sort: SortState) {
        _uiState.update { it.copy(query = it.query.copy(sort = sort), sortSheetVisible = false) }
        viewModelScope.launch { setSortType(sort.value) }
    }

    private fun setSortSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(sortSheetVisible = visible) }
    }

    private fun setSelectionMode(enabled: Boolean) {
        _uiState.update {
            it.copy(selectionMode = enabled, selectedIds = if (enabled) it.selectedIds else emptySet())
        }
    }

    private fun toggleSelection(id: String) {
        _uiState.update { state ->
            val selected = if (id in state.selectedIds) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = selected)
        }
    }

    private fun selectAll() {
        _uiState.update { it.copy(selectedIds = items.value.map { item -> item.mId }.toSet()) }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    private fun deleteSelected() {
        val ids = _uiState.value.selectedIds
        val targets = items.value.filter { it.mId in ids }

        viewModelScope.launch {
            targets.forEach { deleteMedia(it) }
            clearSelection()
        }
    }

    private fun delete(item: VideoTaskItem) {
        viewModelScope.launch { deleteMedia(item) }
    }

    private fun rename(context: Context, item: VideoTaskItem, newName: String) {
        viewModelScope.launch {
            val isImage = item.mimeType.startsWith("image")
            if (!renameMedia(context, item, newName, isImage)) {
                _effect.send(LibraryContract.Effect.RenameFailed)
            }
        }
    }

    /**
     * Moves the selected items into, or out of, the PIN-protected area. Each item's file is
     * physically relocated, so a failure for one item must not be reported as success.
     */
    private fun moveSelectedToPrivate(context: Context, makePrivate: Boolean) {
        val ids = _uiState.value.selectedIds
        val targets = items.value.filter { it.mId in ids }
        if (targets.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    privateMoveProgress = PrivateMoveProgress(
                        movingToPrivate = makePrivate,
                        completed = 0,
                        total = targets.size,
                    ),
                )
            }

            var allMoved = true
            targets.forEachIndexed { index, item ->
                val moved = setMediaPrivate(context, item, makePrivate) { fraction ->
                    _uiState.update { state ->
                        state.copy(
                            privateMoveProgress = state.privateMoveProgress?.copy(
                                currentFileFraction = fraction,
                            ),
                        )
                    }
                }
                if (!moved) allMoved = false

                _uiState.update { state ->
                    state.copy(
                        privateMoveProgress = state.privateMoveProgress?.copy(
                            completed = index + 1,
                            currentFileFraction = 0f,
                        ),
                    )
                }
            }

            _uiState.update { it.copy(privateMoveProgress = null) }
            clearSelection()

            if (!allMoved) {
                _effect.send(LibraryContract.Effect.MoveToPrivateFailed)
            }
        }
    }
}
