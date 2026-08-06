package com.smarttool.videodownloader.ui.privateVideo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.dialog.DialogConfirmDelete
import com.smarttool.videodownloader.dialog.DialogRename
import com.smarttool.videodownloader.feature.library.di.PrivateLibrary
import com.smarttool.videodownloader.feature.library.presentation.LibraryScreen
import com.smarttool.videodownloader.feature.library.presentation.LibraryViewModel
import com.smarttool.videodownloader.feature.library.presentation.MediaActionSheet
import com.smarttool.videodownloader.feature.library.presentation.SelectionActions
import com.smarttool.videodownloader.feature.library.presentation.SortSheet
import com.smarttool.videodownloader.ui.media.PlayMediaActivity
import com.smarttool.videodownloader.core.file.IntentUtil
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class PrivateVideoActivity : BaseComposeActivity() {

    private val viewModel: LibraryViewModel by viewModel(PrivateLibrary)

    private val intentUtil: IntentUtil by inject()

    private var menuTarget by mutableStateOf<VideoTaskItem?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val items by viewModel.items.collectAsStateWithLifecycle()

            AppTheme {
                LibraryScreen(
                    title = getString(R.string.string_private_file),
                    state = state,
                    items = items,
                    showAd = items.isNotEmpty(),
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
                        onTogglePrivate = { viewModel.moveSelectedToPrivate(false) },
                        onCancel = viewModel::clearSelection,
                        privateActionIconRes = R.drawable.ic_security,
                    ),
                    onBack = { finish() },
                    trailingAction = {
                        Image(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).clickable(onClick = ::openPicker),
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
                        privateActionLabel = R.string.string_remove_from_private,
                        onRename = { withDismissedSheet { showRenameDialog(target) } },
                        onShare = { withDismissedSheet { share(target) } },
                        onTogglePrivate = {
                            withDismissedSheet {
                                viewModel.toggleSelection(target.mId)
                                viewModel.moveSelectedToPrivate(false)
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

    private fun openPicker() {
        startActivity(Intent(this, SelectVideoActivity::class.java))
    }

    private fun openMedia(item: VideoTaskItem) {
        startActivity(
            Intent(this, PlayMediaActivity::class.java).apply {
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

    private fun share(item: VideoTaskItem) {
        intentUtil.shareVideo(this, File(item.filePath).toUri())
    }

    private fun showRenameDialog(item: VideoTaskItem) {
        DialogRename(this, item.fileName) { newName ->
            viewModel.rename(this, item, newName) {
                Toast.makeText(
                    this,
                    getString(R.string.string_invalid_data),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }.show()
    }

    private fun showDeleteDialog(item: VideoTaskItem) {
        DialogConfirmDelete(this) { viewModel.delete(item) }.show()
    }
}
