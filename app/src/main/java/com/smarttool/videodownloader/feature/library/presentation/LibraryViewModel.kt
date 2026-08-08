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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sort = SortState.getSortState(getSortType())
            _uiState.update { it.copy(query = it.query.copy(sort = sort)) }
        }
    }

    val items: StateFlow<List<VideoTaskItem>> = _uiState
        .flatMapLatest { state ->
            if (isPrivate) observePrivateLibrary(state.query) else observeLibrary(state.query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onFilterChange(filter: MediaFilter) {
        _uiState.update { it.copy(query = it.query.copy(filter = filter)) }
        clearSelection()
    }

    fun onSearchChange(search: String) {
        _uiState.update { it.copy(query = it.query.copy(search = search)) }
    }

    fun setSearchVisible(visible: Boolean) {
        _uiState.update {
            it.copy(
                searchVisible = visible,
                query = if (visible) it.query else it.query.copy(search = ""),
            )
        }
    }

    fun onSortChange(sort: SortState) {
        _uiState.update { it.copy(query = it.query.copy(sort = sort), sortSheetVisible = false) }
        viewModelScope.launch { setSortType(sort.value) }
    }

    fun setSortSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(sortSheetVisible = visible) }
    }

    fun setSelectionMode(enabled: Boolean) {
        _uiState.update {
            it.copy(selectionMode = enabled, selectedIds = if (enabled) it.selectedIds else emptySet())
        }
    }

    fun toggleSelection(id: String) {
        _uiState.update { state ->
            val selected = if (id in state.selectedIds) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = selected)
        }
    }

    fun selectAll() {
        _uiState.update { it.copy(selectedIds = items.value.map { item -> item.mId }.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds
        val targets = items.value.filter { it.mId in ids }

        viewModelScope.launch {
            targets.forEach { deleteMedia(it) }
            clearSelection()
        }
    }

    fun delete(item: VideoTaskItem) {
        viewModelScope.launch { deleteMedia(item) }
    }

    fun rename(context: Context, item: VideoTaskItem, newName: String, onFailure: () -> Unit) {
        viewModelScope.launch {
            val isImage = item.mimeType.startsWith("image")
            if (!renameMedia(context, item, newName, isImage)) onFailure()
        }
    }

    /** Moves the selected items into, or out of, the PIN-protected area. */
    fun moveSelectedToPrivate(makePrivate: Boolean) {
        val ids = _uiState.value.selectedIds

        viewModelScope.launch {
            ids.forEach { setMediaPrivate(it, makePrivate) }
            clearSelection()
        }
    }
}
