package com.smarttool.videodownloader.ui.privateVideo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.library.presentation.LibraryScreen
import com.smarttool.videodownloader.feature.library.presentation.LibraryViewModel
import com.smarttool.videodownloader.feature.library.presentation.SortSheet
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Picker over the normal library: every row is selectable, and confirming moves the
 * chosen items into the PIN-protected area.
 */
class SelectVideoActivity : BaseComposeActivity() {

    private val viewModel: LibraryViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val items by viewModel.items.collectAsStateWithLifecycle()

            // This screen exists only to select, so selection mode is always on.
            LaunchedEffect(Unit) { viewModel.setSelectionMode(true) }

            AppTheme {
                LibraryScreen(
                    title = getString(R.string.string_add_to_private),
                    state = state,
                    items = items,
                    showAd = false,
                    onFilterChange = viewModel::onFilterChange,
                    onSearchChange = viewModel::onSearchChange,
                    onSearchVisibleChange = viewModel::setSearchVisible,
                    onOpenSort = { viewModel.setSortSheetVisible(true) },
                    onItemClick = { viewModel.toggleSelection(it.mId) },
                    onItemMenu = { viewModel.toggleSelection(it.mId) },
                    onToggleSelection = viewModel::toggleSelection,
                    onBack = { finish() },
                    trailingAction = {
                        if (state.selectedIds.isNotEmpty()) {
                            Image(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).clickable(onClick = ::confirm),
                            )
                        }
                    },
                )

                if (state.sortSheetVisible) {
                    SortSheet(
                        selected = state.query.sort,
                        onSelect = viewModel::onSortChange,
                        onDismiss = { viewModel.setSortSheetVisible(false) },
                    )
                }
            }
        }
    }

    private fun confirm() {
        viewModel.moveSelectedToPrivate(true)
        finish()
    }
}
