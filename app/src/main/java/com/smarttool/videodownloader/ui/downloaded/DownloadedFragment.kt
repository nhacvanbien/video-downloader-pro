package com.smarttool.videodownloader.ui.downloaded

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.dialog.DialogConfirmDelete
import com.smarttool.videodownloader.dialog.DialogRename
import com.smarttool.videodownloader.feature.library.presentation.LibraryScreen
import com.smarttool.videodownloader.feature.library.presentation.LibraryViewModel
import com.smarttool.videodownloader.feature.library.presentation.MediaActionSheet
import com.smarttool.videodownloader.feature.library.presentation.SelectionActions
import com.smarttool.videodownloader.feature.library.presentation.SortSheet
import com.smarttool.videodownloader.ui.media.PlayMediaActivity
import com.smarttool.videodownloader.ui.pin.PinActivity
import com.smarttool.videodownloader.core.file.IntentUtil
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.core.net.toUri
import java.io.File

class DownloadedFragment : Fragment() {

    private val viewModel: LibraryViewModel by viewModel()

    private val intentUtil: IntentUtil by inject()

    /** Item whose action sheet is open, or null when the sheet is closed. */
    private var menuTarget by mutableStateOf<VideoTaskItem?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val items by viewModel.items.collectAsStateWithLifecycle()

            AppTheme {
                LibraryScreen(
                    title = getString(R.string.string_downloaded),
                    state = state,
                    items = items,
                    showAd = true,
                    onFilterChange = viewModel::onFilterChange,
                    onSearchChange = viewModel::onSearchChange,
                    onSearchVisibleChange = viewModel::setSearchVisible,
                    onOpenSort = { viewModel.setSortSheetVisible(true) },
                    onItemClick = ::openMedia,
                    onItemMenu = { menuTarget = it },
                    onToggleSelection = viewModel::toggleSelection,
                    onItemLongClick = { item ->
                        viewModel.setSelectionMode(true)
                        viewModel.toggleSelection(item.mId)
                    },
                    selectionActions = SelectionActions(
                        onSelectAll = viewModel::selectAll,
                        onDelete = viewModel::deleteSelected,
                        onTogglePrivate = { viewModel.moveSelectedToPrivate(true) },
                        onCancel = viewModel::clearSelection,
                        privateActionIconRes = R.drawable.ic_security,
                    ),
                    trailingAction = {
                        Image(
                            painter = painterResource(R.drawable.ic_security),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).clickable(onClick = ::openPrivateArea),
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
                        privateActionLabel = R.string.string_move_to_private,
                        onRename = { withDismissedSheet { showRenameDialog(target) } },
                        onShare = { withDismissedSheet { share(target) } },
                        onTogglePrivate = {
                            withDismissedSheet {
                                viewModel.toggleSelection(target.mId)
                                viewModel.moveSelectedToPrivate(true)
                            }
                        },
                        onDelete = { withDismissedSheet { showDeleteDialog(target) } },
                        onDismiss = { menuTarget = null },
                    )
                }
            }
        }
    }

    private inline fun withDismissedSheet(action: () -> Unit) {
        menuTarget = null
        action()
    }

    private fun openMedia(item: VideoTaskItem) {
        startActivity(
            Intent(requireContext(), PlayMediaActivity::class.java).apply {
                putExtra(PlayMediaActivity.VIDEO_URL, item.filePath)
                putExtra(PlayMediaActivity.VIDEO_NAME, item.fileName)
                putExtra(
                    PlayMediaActivity.ITEM_TYPE,
                    if (item.mimeType.startsWith("image")) "image" else "video",
                )
                putExtra(PlayMediaActivity.IS_DOWNLOADED, true)
            },
        )
    }

    private fun openPrivateArea() {
        startActivity(Intent(requireContext(), PinActivity::class.java))
    }

    private fun share(item: VideoTaskItem) {
        intentUtil.shareVideo(requireContext(), File(item.filePath).toUri())
    }

    private fun showRenameDialog(item: VideoTaskItem) {
        DialogRename(requireContext(), item.fileName) { newName ->
            viewModel.rename(requireContext(), item, newName) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.string_invalid_data),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }.show()
    }

    private fun showDeleteDialog(item: VideoTaskItem) {
        DialogConfirmDelete(requireContext()) { viewModel.delete(item) }.show()
    }
}
