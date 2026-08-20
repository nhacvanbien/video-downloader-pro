package com.smarttool.videodownloader.feature.downloads.presentation

import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
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
import com.smarttool.videodownloader.core.ui.theme.WarnInk
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
            item.isWaitingForWifi -> WaitingForWifiRow(item, selectionMode, selected, onClick, onMenu)
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

// Active rows (downloading, failed, waiting-for-wifi) all share this height so the list doesn't
// jump between row sizes. Sized to fit the larger 40dp tap targets these rows use for their
// primary action buttons (pause/cancel/retry) rather than the smaller 30dp used elsewhere.
private val DownloadActiveRowHeight = 84.dp
private val ActiveRowButtonSize = 40.dp
private val OverflowMenuGlyphSize = 20.dp
private val OverflowMenuButtonWidth = 26.dp

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
    val isAudio = info.videoInfo.isAudioOnly

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DownloadActiveRowHeight)
            .clip(ShapeLg)
            .background(WarnSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) SelectionDot(selected)

        MediaThumbnail(
            filePath = info.videoInfo.thumbnail,
            mediaType = if (isAudio) MediaKind.AUDIO else MediaKind.VIDEO,
            modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(ShapeMd),
        )

        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        .size(ActiveRowButtonSize)
                        .clip(ShapePill)
                        .background(Surface)
                        .clickable { onPauseResume(info) }
                        .padding(9.dp),
                )

                Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(ActiveRowButtonSize)
                        .clip(ShapePill)
                        .background(Surface)
                        .clickable { onCancel(info) }
                        .padding(11.dp),
                )
            }

            if (info.isFetchingInfo) {
                // No byte count exists yet (queued, or yt-dlp still extracting metadata) —
                // a "0%" bar here would read as stalled instead of as work in progress.
                Text(
                    text = stringResource(
                        if (isAudio) {
                            R.string.string_fetching_audio_info
                        } else {
                            R.string.string_fetching_video_info
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    // Progress updates arrive roughly once a second from the download worker;
                    // animating the fraction smooths the bar's motion between those steps instead
                    // of snapping it forward.
                    val animatedProgress by animateFloatAsState(
                        targetValue = info.progress / 100f,
                        animationSpec = tween(durationMillis = 400),
                        label = "downloadProgress",
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        color = Warn,
                        trackColor = Warn.copy(alpha = 0.25f),
                        modifier = Modifier.weight(1f).height(4.dp).clip(ShapePill),
                    )

                    if (info.speedFormatted.isNotEmpty()) {
                        Text(
                            text = info.speedFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    Text(
                        text = "${info.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Warn,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
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
            .height(DownloadActiveRowHeight)
            .clip(ShapeLg)
            .background(Error.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) SelectionDot(selected)

        MediaThumbnail(
            filePath = item.progressInfo.videoInfo.thumbnail,
            mediaType = if (item.progressInfo.videoInfo.isAudioOnly) MediaKind.AUDIO else MediaKind.VIDEO,
            modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(ShapeMd),
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
                text = stringResource(R.string.string_download_failed_short),
                style = MaterialTheme.typography.labelSmall,
                color = Error,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        // Same tap target size as the pause/cancel buttons on the downloading row, so all
        // active-row action buttons feel consistent.
        Image(
            painter = painterResource(R.drawable.ic_reload),
            contentDescription = null,
            colorFilter = ColorFilter.tint(PriInk),
            modifier = Modifier
                .size(ActiveRowButtonSize)
                .clip(ShapePill)
                .background(Error)
                .clickable { onRetry(item.progressInfo) }
                .padding(9.dp),
        )

        OverflowMenuButton(onMenu)
    }
}

/**
 * The glyph sits flush against the tap target's trailing edge so it lands the same distance
 * from the row border as the completed row's menu button, which has no tap-target padding.
 * The target is narrower than it is tall: the slack all falls on the leading side, where it
 * would otherwise push the neighbouring button away from the glyph.
 */
@Composable
private fun OverflowMenuButton(onMenu: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = OverflowMenuButtonWidth, height = ActiveRowButtonSize)
            .clip(ShapePill)
            .clickable(onClick = onMenu),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_more),
            contentDescription = null,
            modifier = Modifier.size(OverflowMenuGlyphSize),
        )
    }
}

@Composable
private fun WaitingForWifiRow(
    item: DownloadListItem.Active,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DownloadActiveRowHeight)
            .clip(ShapeLg)
            .background(WarnSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) SelectionDot(selected)

        Image(
            painter = painterResource(R.drawable.ic_wifi_off),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(ShapePill)
                .background(Surface)
                .padding(9.dp),
        )

        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = stringResource(R.string.string_waiting_for_wifi),
                style = MaterialTheme.typography.labelSmall,
                color = WarnInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Text(
            text = stringResource(R.string.string_queued),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextColor,
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(ShapePill)
                .background(Surface)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )

        OverflowMenuButton(onMenu)
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
            mediaType = MediaKind.forFile(videoTaskItem.mimeType, videoTaskItem.fileName),
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
