package com.smarttool.videodownloader.feature.downloads.presentation

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.ui.components.MediaKind
import com.smarttool.videodownloader.core.ui.components.MediaThumbnail
import com.smarttool.videodownloader.core.ui.components.RetainedAndroidView
import com.smarttool.videodownloader.core.ui.components.SheetCornerRadius
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Error
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriInk
import com.smarttool.videodownloader.core.ui.theme.PriSoft
import com.smarttool.videodownloader.core.ui.theme.ShapeLg
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.core.ui.theme.Warn
import com.smarttool.videodownloader.core.ui.theme.WarnSoft
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.library.domain.model.MediaFilter
import com.smarttool.videodownloader.feature.library.domain.model.SortState
import com.smarttool.videodownloader.feature.library.presentation.MediaFilterChipRow
import com.smarttool.videodownloader.feature.library.presentation.MediaSearchBar
import com.smarttool.videodownloader.feature.library.presentation.sortStateIconRes

@Composable
fun DownloadsScreen(
    pasteState: ProcessingUiState,
    items: List<DownloadListItem>,
    filesCount: Int,
    activeFilter: MediaFilter,
    search: String,
    sortState: SortState,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    selectedStorageLabel: String?,
    detectionWebView: View,
    onUrlChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onGuideClick: () -> Unit,
    onFilterChange: (MediaFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenSort: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onOpenPrivateArea: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveSelectedToPrivate: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onItemClick: (DownloadListItem) -> Unit,
    onItemMenu: (DownloadListItem) -> Unit,
    onPauseResume: (ProgressInfo) -> Unit,
    onCancel: (ProgressInfo) -> Unit,
    onRetry: (ProgressInfo) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DownloadsHeader(
            selectionMode = selectionMode,
            hasSelection = selectedIds.isNotEmpty(),
            onToggleSelectionMode = onToggleSelectionMode,
            onOpenPrivateArea = onOpenPrivateArea,
            onDeleteSelected = onDeleteSelected,
            onGuideClick = onGuideClick,
        )

//        PasteRow(
//            state = pasteState,
//            onUrlChange = onUrlChange,
//            onPasteClick = onPasteClick,
//            onDownloadClick = onDownloadClick,
//        )

        // Off-screen WebView that loads the pasted URL so the detection pipeline can
        // scan it. It must stay attached and laid out, so it is sized to 1dp rather
        // than removed from composition.
        RetainedAndroidView(view = detectionWebView, modifier = Modifier.size(1.dp))

        MediaFilterChipRow(selected = activeFilter, onSelect = onFilterChange)

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius))
                .background(Surface),
        ) {
            MediaSearchBar(
                search = search,
                onSearchChange = onSearchChange,
                hint = stringResource(R.string.string_search_downloads),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (selectionMode) {
                        stringResource(R.string.string_num_selected, selectedIds.size.toString())
                    } else {
                        stringResource(R.string.string_num_files, filesCount.toString())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    modifier = Modifier.weight(1f),
                )

                if (!selectionMode) {
                    Row(
                        modifier = Modifier.clickable(onClick = onOpenSort),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(sortStateIconRes(sortState)),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(TextColor),
                            modifier = Modifier.size(18.dp),
                        )

                        Text(
                            text = stringResource(R.string.string_sort),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                            fontWeight = FontWeight.Bold,
                            color = TextColor,
                            modifier = Modifier.padding(start = 5.dp),
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                EmptyDownloads()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(items, key = { it.javaClass.name + ":" + it.id }) { item ->
                        DownloadListRow(
                            item = item,
                            selectionMode = selectionMode,
                            selected = item.id in selectedIds,
                            onClick = {
                                if (selectionMode) onToggleSelection(item.id) else onItemClick(item)
                            },
                            onMenu = { onItemMenu(item) },
                            onPauseResume = onPauseResume,
                            onCancel = onCancel,
                            onRetry = onRetry,
                        )
                    }
                }
            }
        }

        if (selectionMode && selectedIds.isNotEmpty()) {
            SelectionActionBar(
                storageLabel = selectedStorageLabel,
                onMoveToPrivate = onMoveSelectedToPrivate,
                onDelete = onDeleteSelected,
            )
        }
    }
}

@Composable
private fun DownloadsHeader(
    selectionMode: Boolean,
    hasSelection: Boolean,
    onToggleSelectionMode: () -> Unit,
    onOpenPrivateArea: () -> Unit,
    onDeleteSelected: () -> Unit,
    onGuideClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                if (selectionMode) R.string.string_select_items else R.string.string_downloads,
            ),
            style = MaterialTheme.typography.titleLarge,
            color = TextColor,
            modifier = Modifier.weight(1f),
        )

        if (!selectionMode) {
            Image(
                painter = painterResource(R.drawable.ic_guide),
                contentDescription = null,
                modifier = Modifier.size(22.dp).clickable(onClick = onGuideClick),
            )

            Box(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .size(34.dp)
                    .clip(ShapePill)
                    .border(1.dp, Border, ShapePill)
                    .clickable(onClick = onOpenPrivateArea),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_security),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(34.dp)
                    .clip(ShapePill)
                    .border(1.dp, Border, ShapePill)
                    .clickable(onClick = onToggleSelectionMode),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_selected),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(ShapePill)
                    .border(1.dp, Border, ShapePill)
                    .clickable(onClick = onToggleSelectionMode),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }

            if (hasSelection) {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(34.dp)
                        .clip(ShapePill)
                        .background(Error)
                        .clickable(onClick = onDeleteSelected),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PasteRow(
    state: ProcessingUiState,
    onUrlChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onDownloadClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(ShapePill)
                .background(Surface)
                .border(1.dp, Border, ShapePill)
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (state.url.isEmpty()) {
                    Text(
                        text = stringResource(R.string.string_paste_link_here),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                    )
                }

                BasicTextField(
                    value = state.url,
                    onValueChange = onUrlChange,
                    singleLine = true,
                    cursorBrush = SolidColor(Pri),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextColor),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = stringResource(R.string.string_paste),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = Pri,
                modifier = Modifier.clickable(onClick = onPasteClick).padding(horizontal = 8.dp),
            )
        }

        DownloadButton(buttonState = state.downloadButtonState, onClick = onDownloadClick)
    }
}

@Composable
private fun DownloadButton(buttonState: DownloadButtonUiState, onClick: () -> Unit) {
    when (buttonState) {
        DownloadButtonUiState.Loading -> Box(
            modifier = Modifier.padding(start = 8.dp).size(40.dp).clip(ShapePill).background(Pri),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = PriInk, strokeWidth = 3.dp, modifier = Modifier.size(20.dp))
        }

        DownloadButtonUiState.Enabled -> Image(
            painter = painterResource(R.drawable.ic_download_enable),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp).size(40.dp).clickable(onClick = onClick),
        )

        DownloadButtonUiState.Disabled -> Image(
            painter = painterResource(R.drawable.ic_download_disable_update),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp).size(40.dp),
        )
    }
}

@Composable
private fun DownloadListRow(
    item: DownloadListItem,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    onPauseResume: (ProgressInfo) -> Unit,
    onCancel: (ProgressInfo) -> Unit,
    onRetry: (ProgressInfo) -> Unit,
) {
    when (item) {
        is DownloadListItem.Active -> when {
            item.isFailed -> FailedRow(item, selectionMode, selected, onClick, onMenu, onRetry)
            item.isWaitingForWifi -> WaitingForWifiRow(item, selectionMode, selected, onClick, onCancel)
            else -> DownloadingRow(item, selectionMode, selected, onClick, onPauseResume, onCancel)
        }

        is DownloadListItem.Completed -> CompletedRow(item, selectionMode, selected, onClick, onMenu)
    }
}

@Composable
private fun SelectionDot(selected: Boolean) {
    Image(
        painter = painterResource(if (selected) R.drawable.ic_dot_selected else R.drawable.ic_dot_normal),
        contentDescription = null,
        modifier = Modifier.size(20.dp).padding(end = 8.dp),
    )
}

// Downloading rows are taller than a single-line row (title + pause/cancel row, then progress
// row). Other row types match this so all items in the list share one row height.
private val DownloadActiveRowHeight = 68.dp

@Composable
private fun DownloadingRow(
    item: DownloadListItem.Active,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onPauseResume: (ProgressInfo) -> Unit,
    onCancel: (ProgressInfo) -> Unit,
) {
    val info = item.progressInfo
    val isPaused = info.downloadStatus == VideoTaskState.PAUSE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeLg)
            .background(WarnSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) SelectionDot(selected)

            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Image(
                painter = painterResource(if (isPaused) R.drawable.ic_play else R.drawable.ic_pause),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(30.dp)
                    .clip(ShapePill)
                    .background(Surface)
                    .clickable { onPauseResume(info) }
                    .padding(6.dp),
            )

            Image(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(30.dp)
                    .clip(ShapePill)
                    .background(Surface)
                    .clickable { onCancel(info) }
                    .padding(8.dp),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            LinearProgressIndicator(
                progress = { info.progress / 100f },
                color = Warn,
                trackColor = Warn.copy(alpha = 0.25f),
                modifier = Modifier.weight(1f).height(4.dp).clip(ShapePill),
            )

            Text(
                text = "${info.progress}%",
                style = MaterialTheme.typography.labelSmall,
                color = Warn,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun FailedRow(
    item: DownloadListItem.Active,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    onRetry: (ProgressInfo) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeLg)
            .background(Error.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) SelectionDot(selected)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = stringResource(R.string.string_download_failed_short),
                style = MaterialTheme.typography.labelSmall,
                color = Error,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Image(
            painter = painterResource(R.drawable.ic_reload),
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .clip(ShapePill)
                .background(Error)
                .clickable { onRetry(item.progressInfo) }
                .padding(7.dp),
        )

        Image(
            painter = painterResource(R.drawable.ic_more),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(30.dp)
                .clickable(onClick = onMenu)
                .padding(7.dp),
        )
    }
}

@Composable
private fun WaitingForWifiRow(
    item: DownloadListItem.Active,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onCancel: (ProgressInfo) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DownloadActiveRowHeight)
            .clip(ShapeLg)
            .background(Muted.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) SelectionDot(selected)

        Image(
            painter = painterResource(R.drawable.ic_wifi_only),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )

        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = stringResource(R.string.string_waiting_for_wifi),
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Image(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .clip(ShapePill)
                .background(Surface)
                .clickable { onCancel(item.progressInfo) }
                .padding(8.dp),
        )
    }
}

@Composable
private fun CompletedRow(
    item: DownloadListItem.Completed,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
) {
    val videoTaskItem = item.videoTaskItem

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DownloadActiveRowHeight)
            .clip(ShapeLg)
            .background(Border.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) SelectionDot(selected)

        MediaThumbnail(
            filePath = videoTaskItem.filePath,
            mediaType = MediaKind.fromMimeType(videoTaskItem.mimeType),
            modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(ShapeMd),
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = FileUtil.getFileSizeReadable(videoTaskItem.fileSize.toDouble()),
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
            )
        }

        Image(
            painter = painterResource(R.drawable.ic_more),
            contentDescription = null,
            modifier = Modifier.size(20.dp).clickable(onClick = onMenu),
        )
    }
}

@Composable
private fun EmptyDownloads() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(ShapeLg).background(PriSoft),
            contentAlignment = Alignment.Center,
        ) {
            Image(painter = painterResource(R.drawable.ic_airboat), contentDescription = null)
        }

        Text(
            text = stringResource(R.string.string_download_not_found),
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp, start = 40.dp, end = 40.dp),
        )
    }
}

@Composable
private fun SelectionActionBar(
    storageLabel: String?,
    onMoveToPrivate: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Surface).border(1.dp, Border).padding(16.dp),
    ) {
        if (storageLabel != null) {
            Text(
                text = storageLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.string_move_to_private),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = Pri,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(ShapePill)
                    .border(1.dp, Pri, ShapePill)
                    .clickable(onClick = onMoveToPrivate)
                    .padding(vertical = 11.dp),
            )

            Text(
                text = stringResource(R.string.string_delete),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = PriInk,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(ShapePill)
                    .background(Error)
                    .clickable(onClick = onDelete)
                    .padding(vertical = 11.dp),
            )
        }
    }
}
