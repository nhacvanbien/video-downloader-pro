package com.smarttool.videodownloader.feature.downloads.presentation

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.file.IntentUtil
import com.smarttool.videodownloader.core.ui.dialogs.DialogConfirmDelete
import com.smarttool.videodownloader.core.ui.dialogs.DialogRename
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.browser.presentation.DetectedVideosContract
import com.smarttool.videodownloader.feature.library.domain.model.MediaFilter
import com.smarttool.videodownloader.feature.library.presentation.LibraryContract
import com.smarttool.videodownloader.feature.library.presentation.LibraryViewModel
import com.smarttool.videodownloader.feature.library.presentation.MediaActionSheet
import com.smarttool.videodownloader.feature.library.presentation.PrivateMoveProgressDialog
import com.smarttool.videodownloader.feature.library.presentation.SortSheet
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File

/**
 * The merged "Downloads" tab: active downloads (from [ProcessingViewModel], owned by the
 * Activity-level [host]) interleaved with completed files (from [LibraryViewModel]) in one
 * list, per the Pinboard redesign. Every download action still goes through
 * [ProcessingViewModel]/`DownloaderGateway` — this route only combines state for display.
 */
@Composable
fun DownloadsRoute(
    host: ProcessingWebViewHost,
    onOpenGuide: () -> Unit,
    onPreviewMedia: (url: String, title: String, headers: String) -> Unit,
    onOpenMedia: (VideoTaskItem) -> Unit,
    onOpenPrivateArea: () -> Unit,
) {
    val context = LocalContext.current
    val processingViewModel = host.processingViewModel
    val detector = host.detector
    val libraryViewModel: LibraryViewModel = koinViewModel()
    val intentUtil: IntentUtil = koinInject()

    LaunchedEffect(Unit) {
        host.onPreviewMedia = onPreviewMedia
    }

    val vmState by processingViewModel.uiState.collectAsStateWithLifecycle()
    val detectorState by detector.uiState.collectAsStateWithLifecycle()
    val activeDownloads by processingViewModel.downloads.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val completedItems by libraryViewModel.items.collectAsStateWithLifecycle()

    val pasteState = ProcessingUiState(
        url = vmState.url,
        downloadButtonState = detectorState.downloadButtonState.toUiState(),
    )

    val mergedItems = remember(
        activeDownloads,
        completedItems,
        libraryState.query.filter,
        libraryState.query.search,
    ) {
        activeDownloads.map { DownloadListItem.Active(it) }
            .filter { it.matches(libraryState.query.filter, libraryState.query.search) } +
            completedItems.map { DownloadListItem.Completed(it) }
    }

    val completedIgnoringType by libraryViewModel.itemsIgnoringTypeFilter.collectAsStateWithLifecycle()

    val filterCounts = remember(activeDownloads, completedIgnoringType, libraryState.query.search) {
        val countedTypes = activeDownloads.map { DownloadListItem.Active(it) }
            .filter { it.matches(MediaFilter.All, libraryState.query.search) }
            .map { it.mediaType } +
            completedIgnoringType.map { MediaFilter.forFile(it.mimeType, it.fileName) }

        mapOf(
            MediaFilter.All to countedTypes.size,
            MediaFilter.Video to countedTypes.count { it == MediaFilter.Video },
            MediaFilter.Audio to countedTypes.count { it == MediaFilter.Audio },
            MediaFilter.Image to countedTypes.count { it == MediaFilter.Image },
        )
    }

    var menuTarget by remember { mutableStateOf<VideoTaskItem?>(null) }
    var failedMenuTarget by remember { mutableStateOf<ProgressInfo?>(null) }

    val closeSheetThen = { action: () -> Unit ->
        menuTarget = null
        action()
    }

    val selectedStorageLabel = if (libraryState.selectionMode && libraryState.selectedIds.isNotEmpty()) {
        val bytes = completedItems.filter { it.mId in libraryState.selectedIds }.sumOf { it.fileSize }
        FileUtil.getFileSizeReadable(bytes.toDouble())
    } else {
        null
    }

    DownloadsScreen(
        pasteState = pasteState,
        items = mergedItems,
        filesCount = mergedItems.size,
        filterCounts = filterCounts,
        activeFilter = libraryState.query.filter,
        search = libraryState.query.search,
        sortState = libraryState.query.sort,
        viewMode = libraryState.viewMode,
        selectionMode = libraryState.selectionMode,
        selectedIds = libraryState.selectedIds,
        selectedStorageLabel = selectedStorageLabel,
        detectionWebView = host.detectionWebView,
        onUrlChange = { processingViewModel.onEvent(ProcessingContract.Event.UrlChange(it)) },
        onPasteClick = { pasteFromClipboard(context, processingViewModel) },
        onDownloadClick = { detector.onEvent(DetectedVideosContract.Event.ShowVideoInfo) },
        onGuideClick = onOpenGuide,
        onFilterChange = { libraryViewModel.onEvent(LibraryContract.Event.FilterChange(it)) },
        onSearchChange = { libraryViewModel.onEvent(LibraryContract.Event.SearchChange(it)) },
        onOpenSort = { libraryViewModel.onEvent(LibraryContract.Event.SetSortSheetVisible(true)) },
        onViewModeChange = { libraryViewModel.onEvent(LibraryContract.Event.SetViewMode(it)) },
        onToggleSelectionMode = {
            libraryViewModel.onEvent(LibraryContract.Event.SetSelectionMode(!libraryState.selectionMode))
        },
        onOpenPrivateArea = onOpenPrivateArea,
        onDeleteSelected = {
            val ids = libraryState.selectedIds
            activeDownloads.filter { it.id in ids }
                .forEach { processingViewModel.onEvent(ProcessingContract.Event.Cancel(it, removeFile = true)) }
            libraryViewModel.onEvent(LibraryContract.Event.DeleteSelected)
        },
        onMoveSelectedToPrivate = {
            libraryViewModel.onEvent(LibraryContract.Event.MoveSelectedToPrivate(context, true))
        },
        onToggleSelection = { libraryViewModel.onEvent(LibraryContract.Event.ToggleSelection(it)) },
        onItemClick = { item ->
            when (item) {
                is DownloadListItem.Completed -> onOpenMedia(item.videoTaskItem)
                is DownloadListItem.Active -> Unit
            }
        },
        onItemMenu = { item ->
            when (item) {
                is DownloadListItem.Completed -> menuTarget = item.videoTaskItem
                is DownloadListItem.Active -> if (item.isFailed) failedMenuTarget = item.progressInfo
            }
        },
        onPauseResume = { info ->
            val event = if (info.downloadStatus == VideoTaskState.PAUSE) {
                ProcessingContract.Event.Resume(info)
            } else {
                ProcessingContract.Event.Pause(info)
            }
            processingViewModel.onEvent(event)
        },
        onCancel = { info ->
            processingViewModel.onEvent(ProcessingContract.Event.Cancel(info, removeFile = true))
        },
        onRetry = { info ->
            processingViewModel.onEvent(ProcessingContract.Event.Resume(info))
        },
    )

    DetectedVideosSheetHost(presenter = host.detected)

    if (host.showNoMediaFound) {
        NoMediaFoundDialog(
            onDismiss = { host.dismissNoMediaFound() },
            onReportIssue = { host.dismissNoMediaFound() },
        )
    }

    LaunchedEffect(Unit) {
        libraryViewModel.effect.collect { effect ->
            when (effect) {
                is LibraryContract.Effect.RenameFailed -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.string_invalid_data),
                        Toast.LENGTH_SHORT,
                    ).show()
                }

                is LibraryContract.Effect.MoveToPrivateFailed -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.string_error),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    libraryState.privateMoveProgress?.let { PrivateMoveProgressDialog(progress = it) }

    if (libraryState.sortSheetVisible) {
        SortSheet(
            selected = libraryState.query.sort,
            onSelect = { libraryViewModel.onEvent(LibraryContract.Event.SortChange(it)) },
            onDismiss = { libraryViewModel.onEvent(LibraryContract.Event.SetSortSheetVisible(false)) },
        )
    }

    menuTarget?.let { target ->
        MediaActionSheet(
            fileName = target.fileName,
            privateActionLabel = R.string.string_move_to_private,
            onRename = {
                closeSheetThen {
                    DialogRename(context, target.fileName) { newName ->
                        libraryViewModel.onEvent(LibraryContract.Event.Rename(context, target, newName))
                    }.show()
                }
            },
            onShare = {
                closeSheetThen { intentUtil.shareFile(context, File(target.filePath).toUri()) }
            },
            onTogglePrivate = {
                closeSheetThen {
                    libraryViewModel.onEvent(LibraryContract.Event.ToggleSelection(target.mId))
                    libraryViewModel.onEvent(LibraryContract.Event.MoveSelectedToPrivate(context, true))
                }
            },
            onDelete = {
                closeSheetThen {
                    DialogConfirmDelete(context) {
                        libraryViewModel.onEvent(LibraryContract.Event.Delete(target))
                    }.show()
                }
            },
            onDismiss = { menuTarget = null },
        )
    }

    failedMenuTarget?.let { target ->
        FailedDownloadActionSheet(
            onRetry = {
                failedMenuTarget = null
                processingViewModel.onEvent(ProcessingContract.Event.Resume(target))
            },
            onDelete = {
                failedMenuTarget = null
                processingViewModel.onEvent(ProcessingContract.Event.Cancel(target, removeFile = true))
            },
            onDismiss = { failedMenuTarget = null },
        )
    }
}

private fun pasteFromClipboard(context: Context, viewModel: ProcessingViewModel) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val hasText = clipboard.hasPrimaryClip() &&
        clipboard.primaryClipDescription
            ?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true

    if (!hasText) {
        Toast.makeText(context, context.getString(R.string.string_no_text_in_clipboard), Toast.LENGTH_SHORT).show()
        return
    }

    val copied = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()

    if (copied.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.string_clipboard_is_empty), Toast.LENGTH_SHORT).show()
        return
    }

    viewModel.onEvent(ProcessingContract.Event.UrlChange(copied))
}
