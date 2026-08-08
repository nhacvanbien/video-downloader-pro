package com.smarttool.videodownloader.feature.library.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.file.IntentUtil
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.core.ui.dialogs.DialogConfirmDelete
import com.smarttool.videodownloader.core.ui.dialogs.DialogRename
import com.smarttool.videodownloader.feature.library.di.PrivateLibrary
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File

/**
 * Downloaded files and the PIN-protected private area — the same list with the
 * "move to private" arrow pointing in opposite directions.
 *
 * [isPrivate] selects the Koin-qualified `PrivateLibrary` ViewModel, which is the only
 * thing that decides which set of files is listed.
 */
@Composable
fun LibraryRoute(
    isPrivate: Boolean,
    trailingIconRes: Int,
    onTrailingAction: () -> Unit,
    onOpenMedia: (VideoTaskItem) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val viewModel: LibraryViewModel =
        koinViewModel(qualifier = if (isPrivate) PrivateLibrary else null)

    val intentUtil: IntentUtil = koinInject()
    val context = LocalContext.current

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()

    /** Item whose action sheet is open, or null when the sheet is closed. */
    var menuTarget by remember { mutableStateOf<VideoTaskItem?>(null) }

    val closeSheetThen = { action: () -> Unit ->
        menuTarget = null
        action()
    }

    LibraryScreen(
        title = stringResource(
            if (isPrivate) R.string.string_private_file else R.string.string_downloaded,
        ),
        state = state,
        items = items,
        showAd = if (isPrivate) items.isNotEmpty() else true,
        onFilterChange = viewModel::onFilterChange,
        onSearchChange = viewModel::onSearchChange,
        onSearchVisibleChange = viewModel::setSearchVisible,
        onOpenSort = { viewModel.setSortSheetVisible(true) },
        onItemClick = onOpenMedia,
        onItemMenu = { menuTarget = it },
        onToggleSelection = viewModel::toggleSelection,
        onItemLongClick = { item ->
            viewModel.setSelectionMode(true)
            viewModel.toggleSelection(item.mId)
        },
        onBack = onBack,
        selectionActions = SelectionActions(
            onSelectAll = viewModel::selectAll,
            onDelete = viewModel::deleteSelected,
            onTogglePrivate = { viewModel.moveSelectedToPrivate(!isPrivate) },
            onCancel = viewModel::clearSelection,
            privateActionIconRes = R.drawable.ic_security,
        ),
        trailingAction = {
            Image(
                painter = painterResource(trailingIconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp).clickable(onClick = onTrailingAction),
            )
        },
    )

    if (state.sortSheetVisible) {
        SortSheet(
            selected = state.query.sort,
            onSelect = viewModel::onSortChange,
            onDismiss = { viewModel.setSortSheetVisible(false) },
        )
    }

    menuTarget?.let { target ->
        MediaActionSheet(
            fileName = target.fileName,
            privateActionLabel = if (isPrivate) {
                R.string.string_remove_from_private
            } else {
                R.string.string_move_to_private
            },
            onRename = {
                closeSheetThen {
                    DialogRename(context, target.fileName) { newName ->
                        viewModel.rename(context, target, newName) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.string_invalid_data),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }.show()
                }
            },
            onShare = {
                closeSheetThen { intentUtil.shareVideo(context, File(target.filePath).toUri()) }
            },
            onTogglePrivate = {
                closeSheetThen {
                    viewModel.toggleSelection(target.mId)
                    viewModel.moveSelectedToPrivate(!isPrivate)
                }
            },
            onDelete = {
                closeSheetThen { DialogConfirmDelete(context) { viewModel.delete(target) }.show() }
            },
            onDismiss = { menuTarget = null },
        )
    }
}
