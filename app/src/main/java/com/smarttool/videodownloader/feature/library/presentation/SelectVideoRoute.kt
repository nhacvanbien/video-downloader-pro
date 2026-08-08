package com.smarttool.videodownloader.feature.library.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import org.koin.androidx.compose.koinViewModel

/**
 * Picker over the normal library: every row is selectable, and confirming moves the
 * chosen items into the PIN-protected area.
 */
@Composable
fun SelectVideoRoute(onBack: () -> Unit) {
    val viewModel: LibraryViewModel = koinViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()

    // This screen exists only to select, so selection mode is always on.
    LaunchedEffect(Unit) { viewModel.onEvent(LibraryContract.Event.SetSelectionMode(true)) }

    LibraryScreen(
        title = stringResource(R.string.string_add_to_private),
        state = state,
        items = items,
        onFilterChange = { viewModel.onEvent(LibraryContract.Event.FilterChange(it)) },
        onSearchChange = { viewModel.onEvent(LibraryContract.Event.SearchChange(it)) },
        onSearchVisibleChange = { viewModel.onEvent(LibraryContract.Event.SetSearchVisible(it)) },
        onOpenSort = { viewModel.onEvent(LibraryContract.Event.SetSortSheetVisible(true)) },
        onItemClick = { viewModel.onEvent(LibraryContract.Event.ToggleSelection(it.mId)) },
        onItemMenu = { viewModel.onEvent(LibraryContract.Event.ToggleSelection(it.mId)) },
        onToggleSelection = { viewModel.onEvent(LibraryContract.Event.ToggleSelection(it)) },
        onBack = onBack,
        trailingAction = {
            if (state.selectedIds.isNotEmpty()) {
                Image(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clickable {
                        viewModel.onEvent(LibraryContract.Event.MoveSelectedToPrivate(true))
                        onBack()
                    },
                )
            }
        },
    )

    if (state.sortSheetVisible) {
        SortSheet(
            selected = state.query.sort,
            onSelect = { viewModel.onEvent(LibraryContract.Event.SortChange(it)) },
            onDismiss = { viewModel.onEvent(LibraryContract.Event.SetSortSheetVisible(false)) },
        )
    }
}
