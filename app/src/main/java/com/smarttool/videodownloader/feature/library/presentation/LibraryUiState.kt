package com.smarttool.videodownloader.feature.library.presentation

import com.smarttool.videodownloader.feature.library.domain.model.LibraryQuery

data class LibraryUiState(
    val query: LibraryQuery = LibraryQuery(),
    val searchVisible: Boolean = false,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val sortSheetVisible: Boolean = false,
)
