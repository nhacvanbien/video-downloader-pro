package com.smarttool.videodownloader.feature.library.presentation

import android.content.Context
import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.feature.library.domain.model.LibraryQuery
import com.smarttool.videodownloader.feature.library.domain.model.LibraryViewMode
import com.smarttool.videodownloader.feature.library.domain.model.MediaFilter
import com.smarttool.videodownloader.feature.library.domain.model.SortState

interface LibraryContract {
    data class State(
        val query: LibraryQuery = LibraryQuery(),
        val selectionMode: Boolean = false,
        val selectedIds: Set<String> = emptySet(),
        val sortSheetVisible: Boolean = false,
        val viewMode: LibraryViewMode = LibraryViewMode.List,
        /** Non-null only while a move to/out of the private area is running. */
        val privateMoveProgress: PrivateMoveProgress? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class FilterChange(val filter: MediaFilter) : Event
        data class SearchChange(val search: String) : Event
        data class SortChange(val sort: SortState) : Event
        data class SetSortSheetVisible(val visible: Boolean) : Event
        data class SetViewMode(val mode: LibraryViewMode) : Event
        data class SetSelectionMode(val enabled: Boolean) : Event
        data class ToggleSelection(val id: String) : Event
        data object SelectAll : Event
        data object ClearSelection : Event
        data object DeleteSelected : Event
        data class Delete(val item: VideoTaskItem) : Event
        data class Rename(val context: Context, val item: VideoTaskItem, val newName: String) : Event
        data class MoveSelectedToPrivate(val context: Context, val makePrivate: Boolean) : Event
    }

    sealed interface Effect : UiEffect {
        /** Mirrors the old `rename(...)`'s `onFailure: () -> Unit` — a signal, no payload. */
        data object RenameFailed : Effect

        /** At least one file could not be moved into or out of the private area. */
        data object MoveToPrivateFailed : Effect
    }
}
