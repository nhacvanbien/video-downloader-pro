package com.smarttool.videodownloader.feature.downloads.presentation

import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
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
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.ElevationCard
import com.smarttool.videodownloader.core.ui.theme.ElevationControl
import com.smarttool.videodownloader.core.ui.theme.ElevationFloatingBar
import com.smarttool.videodownloader.core.ui.theme.Error
import com.smarttool.videodownloader.core.ui.theme.ErrorSoft
import com.smarttool.videodownloader.core.ui.theme.Info
import com.smarttool.videodownloader.core.ui.theme.InfoSoft
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriInk
import com.smarttool.videodownloader.core.ui.theme.PriSoft
import com.smarttool.videodownloader.core.ui.theme.ShapeLg
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Success
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.core.ui.theme.softShadow
import com.smarttool.videodownloader.core.ui.theme.WarnInk
import com.smarttool.videodownloader.core.ui.theme.WarnSoft
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.library.domain.model.LibraryViewMode
import com.smarttool.videodownloader.feature.library.domain.model.MediaFilter
import com.smarttool.videodownloader.feature.library.domain.model.SortState
import com.smarttool.videodownloader.feature.library.presentation.sortStateShortLabelRes

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DownloadsScreen(
    pasteState: ProcessingUiState,
    items: List<DownloadListItem>,
    filesCount: Int,
    filterCounts: Map<MediaFilter, Int>,
    activeFilter: MediaFilter,
    search: String,
    sortState: SortState,
    viewMode: LibraryViewMode,
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
    onViewModeChange: (LibraryViewMode) -> Unit,
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
            filesCount = filesCount,
            selectionMode = selectionMode,
            selectedCount = selectedIds.size,
            hasSelection = selectedIds.isNotEmpty(),
            onToggleSelectionMode = onToggleSelectionMode,
            onOpenPrivateArea = onOpenPrivateArea,
            onDeleteSelected = onDeleteSelected,
        )

        // Off-screen WebView that loads the pasted URL so the detection pipeline can
        // scan it. It must stay attached and laid out, so it is sized to 1dp rather
        // than removed from composition.
        RetainedAndroidView(view = detectionWebView, modifier = Modifier.size(1.dp))

        DownloadFilterChipRow(
            selected = activeFilter,
            counts = filterCounts,
            onSelect = onFilterChange,
        )

        DownloadsSearchRow(search = search, onSearchChange = onSearchChange)

        DownloadsMetaRow(
            sortState = sortState,
            viewMode = viewMode,
            onOpenSort = onOpenSort,
            onViewModeChange = onViewModeChange,
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (items.isEmpty()) {
                EmptyDownloads()
            } else {
                val listState = rememberLazyListState()
                val gridState = rememberLazyGridState()

                // Without this the two layouts hold independent scroll offsets, so after
                // scrolling down a switch would fly every row in from off-screen.
                LaunchedEffect(viewMode) {
                    if (viewMode == LibraryViewMode.Grid) {
                        gridState.scrollToItem(listState.firstVisibleItemIndex)
                    } else {
                        listState.scrollToItem(gridState.firstVisibleItemIndex)
                    }
                }

                SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = viewMode,
                        transitionSpec = {
                            fadeIn(tween(ItemMorphMs)) togetherWith fadeOut(tween(ItemMorphMs))
                        },
                        label = "downloadsViewMode",
                    ) { mode ->
                        if (mode == LibraryViewMode.Grid) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = gridState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = ListContentPadding,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(items, key = { it.uiKey }) { item ->
                                    DownloadGridTile(
                                        item = item,
                                        selectionMode = selectionMode,
                                        selected = item.id in selectedIds,
                                        onClick = {
                                            if (selectionMode) onToggleSelection(item.id) else onItemClick(item)
                                        },
                                        onMenu = { onItemMenu(item) },
                                        modifier = Modifier.morphBetweenViewModes(
                                            sharedScope = this@SharedTransitionLayout,
                                            visibilityScope = this@AnimatedContent,
                                            key = item.uiKey,
                                        ),
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = ListContentPadding,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(items, key = { it.uiKey }) { item ->
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
                                        modifier = Modifier.morphBetweenViewModes(
                                            sharedScope = this@SharedTransitionLayout,
                                            visibilityScope = this@AnimatedContent,
                                            key = item.uiKey,
                                        ),
                                    )
                                }
                            }
                        }
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

private const val ItemMorphMs = 420

/**
 * Identity for both the lazy-layout key and the shared-element match.
 *
 * The class name is not decoration: an active row's id comes from `ProgressInfo` and a completed
 * row's from `VideoTaskItem`, two separate id spaces that can collide — and a lazy layout throws
 * on a duplicate key. The same item keeps the same class in list and grid, so this still pairs
 * the two halves of the morph.
 */
private val DownloadListItem.uiKey: String
    get() = javaClass.name + ":" + id

private val ListContentPadding =
    PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)

/**
 * Ties a row to its counterpart in the other view mode, so switching list/grid flies each card
 * from where it was to where it belongs instead of cross-fading the whole list.
 *
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.morphBetweenViewModes(
    sharedScope: SharedTransitionScope,
    visibilityScope: AnimatedVisibilityScope,
    key: String,
): Modifier = with(sharedScope) {
    sharedBounds(
        sharedContentState = rememberSharedContentState(key = key),
        animatedVisibilityScope = visibilityScope,
        enter = fadeIn(tween(ItemMorphMs)),
        exit = fadeOut(tween(ItemMorphMs)),
        boundsTransform = { _, _ -> tween(ItemMorphMs, easing = FastOutSlowInEasing) },
        resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopStart,
        ),
    )
}

@Composable
private fun DownloadsHeader(
    filesCount: Int,
    selectionMode: Boolean,
    selectedCount: Int,
    hasSelection: Boolean,
    onToggleSelectionMode: () -> Unit,
    onOpenPrivateArea: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    if (selectionMode) R.string.string_select_items else R.string.string_downloads,
                ),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                fontWeight = FontWeight.ExtraBold,
                color = TextColor,
            )

            Text(
                text = if (selectionMode) {
                    stringResource(R.string.string_num_selected, selectedCount.toString())
                } else {
                    stringResource(R.string.string_downloads_file_count, filesCount.toString())
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                color = Muted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (!selectionMode) {
            HeaderIconButton(
                iconRes = R.drawable.ic_folder_lock,
                onClick = onOpenPrivateArea,
            )

            HeaderIconButton(
                iconRes = R.drawable.ic_tune,
                onClick = onToggleSelectionMode,
                modifier = Modifier.padding(start = 10.dp),
            )
        } else {
            HeaderIconButton(
                iconRes = R.drawable.ic_close,
                onClick = onToggleSelectionMode,
                iconSize = 15.dp,
            )

            if (hasSelection) {
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(HeaderButtonSize)
                        .clip(ShapePill)
                        .background(Error)
                        .clickable(onClick = onDeleteSelected),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private val HeaderButtonSize = 42.dp

@Composable
private fun HeaderIconButton(
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 19.dp,
) {
    Box(
        modifier = modifier
            .size(HeaderButtonSize)
            .softShadow(ElevationControl, ShapePill)
            .clip(ShapePill)
            .background(Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = TextColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

private val FilterChipOptions = listOf(
    Triple(MediaFilter.All, R.string.string_all, R.drawable.ic_chip_all),
    Triple(MediaFilter.Video, R.string.string_video, R.drawable.ic_chip_video),
    Triple(MediaFilter.Audio, R.string.string_audio, R.drawable.ic_chip_audio),
    Triple(MediaFilter.Image, R.string.string_image, R.drawable.ic_chip_image),
)

/**
 * Downloads-tab variant of the media filter chips: each chip carries its own icon and a count
 * bubble. Kept separate from the shared [com.smarttool.videodownloader.feature.library.presentation.MediaFilterChipRow]
 * because the library screens have no per-type counts to show.
 */
@Composable
private fun DownloadFilterChipRow(
    selected: MediaFilter,
    counts: Map<MediaFilter, Int>,
    onSelect: (MediaFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        FilterChipOptions.forEach { (filter, labelRes, iconRes) ->
            val active = filter == selected

            Row(
                modifier = Modifier
                    .softShadow(ElevationControl, ShapePill)
                    .clip(ShapePill)
                    .background(if (active) PriSoft else Surface)
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = if (active) Pri else Muted,
                    modifier = Modifier.size(14.dp),
                )

                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (active) Pri else Muted,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 5.dp),
                )

                Text(
                    text = (counts[filter] ?: 0).toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (active) PriInk else Muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .clip(ShapePill)
                        .background(if (active) Pri else Border.copy(alpha = 0.55f))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp),
                )
            }
        }
    }
}

@Composable
private fun DownloadsSearchRow(search: String, onSearchChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .softShadow(ElevationControl, ShapePill)
                .clip(ShapePill)
                .background(Surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(17.dp),
            )

            Box(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                if (search.isEmpty()) {
                    Text(
                        text = stringResource(R.string.string_search_in_downloads),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                BasicTextField(
                    value = search,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    cursorBrush = SolidColor(Pri),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = TextColor,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DownloadsMetaRow(
    sortState: SortState,
    viewMode: LibraryViewMode,
    onOpenSort: () -> Unit,
    onViewModeChange: (LibraryViewMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(ShapePill)
                .clickable(onClick = onOpenSort)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sort_arrows),
                contentDescription = null,
                tint = TextColor,
                modifier = Modifier.size(16.dp),
            )

            Text(
                text = stringResource(
                    R.string.string_sort_by_value,
                    stringResource(sortStateShortLabelRes(sortState)),
                ),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold,
                color = TextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp),
            )

            Icon(
                painter = painterResource(R.drawable.ic_arrow_down),
                contentDescription = null,
                tint = TextColor,
                modifier = Modifier.padding(start = 4.dp).size(14.dp),
            )
        }

        Row(
            modifier = Modifier
                .softShadow(ElevationControl, ShapeMd)
                .clip(ShapeMd)
                .background(Surface)
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ViewModeButton(
                iconRes = R.drawable.ic_view_list,
                active = viewMode == LibraryViewMode.List,
                onClick = { onViewModeChange(LibraryViewMode.List) },
            )

            ViewModeButton(
                iconRes = R.drawable.ic_view_grid,
                active = viewMode == LibraryViewMode.Grid,
                onClick = { onViewModeChange(LibraryViewMode.Grid) },
            )
        }
    }
}

@Composable
private fun ViewModeButton(iconRes: Int, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 30.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) PriSoft else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (active) Pri else Muted,
            modifier = Modifier.size(17.dp),
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
    modifier: Modifier = Modifier,
) {
    when (item) {
        is DownloadListItem.Active -> when {
            item.isFailed -> DownloadCard(
                item = item,
                selectionMode = selectionMode,
                selected = selected,
                onClick = onClick,
                onMenu = onMenu,
                containerColor = ErrorSoft,
                modifier = modifier,
            ) {
                FailedStatus(onRetry = { onRetry(item.progressInfo) })
            }

            item.isWaitingForWifi -> DownloadCard(
                item = item,
                selectionMode = selectionMode,
                selected = selected,
                onClick = onClick,
                onMenu = onMenu,
                containerColor = WarnSoft,
                modifier = modifier,
            ) {
                WaitingForWifiStatus()
            }

            else -> DownloadCard(
                item = item,
                selectionMode = selectionMode,
                selected = selected,
                onClick = onClick,
                onMenu = onMenu,
                containerColor = WarnSoft,
                modifier = modifier,
                status = { DownloadingStatus(item.progressInfo) },
                trailing = {
                    val isPaused = item.progressInfo.downloadStatus == VideoTaskState.PAUSE
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RowActionButton(
                            iconRes = if (isPaused) {
                                R.drawable.ic_play_glyph
                            } else {
                                R.drawable.ic_pause_glyph
                            },
                            onClick = { onPauseResume(item.progressInfo) },
                        )

                        RowActionButton(
                            iconRes = R.drawable.ic_close,
                            onClick = { onCancel(item.progressInfo) },
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                },
            )
        }

        is DownloadListItem.Completed -> DownloadCard(
            item = item,
            selectionMode = selectionMode,
            selected = selected,
            onClick = onClick,
            onMenu = onMenu,
            modifier = modifier,
        ) {
            CompletedStatus()
        }
    }
}


@Composable
private fun DownloadCard(
    item: DownloadListItem,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    containerColor: Color = Surface,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    status: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .softShadow(ElevationCard, ShapeLg)
            .clip(ShapeLg)
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) SelectionDot(selected)

        DownloadThumbnail(item, modifier = Modifier.width(104.dp).height(66.dp))

        // Everything right of the thumbnail shares one column, so the status line and the
        // progress bar run all the way to the card's right edge rather than stopping short
        // of the pause and cancel buttons.
        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Bold,
                        color = TextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    DownloadMetaLine(item)
                }

                if (trailing != null) {
                    Box(modifier = Modifier.padding(start = 6.dp)) { trailing() }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(width = 24.dp, height = 34.dp)
                            .clickable(onClick = onMenu),
                    )
                }
            }

            if (status != null) {
                Box(modifier = Modifier.padding(top = 6.dp)) { status() }
            }
        }
    }
}

@Composable
private fun DownloadMetaLine(item: DownloadListItem) {
    val meta = remember(item) {
        listOfNotNull(
            item.sourceLabel,
            item.formatLabel,
            item.sizeBytes.takeIf { it > 0 }?.let { FileUtil.getFileSizeReadable(it.toDouble()) },
        ).joinToString("  •  ")
    }

    if (meta.isEmpty()) return

    Text(
        text = meta,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
        color = Muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 3.dp),
    )
}

@Composable
private fun DownloadThumbnail(item: DownloadListItem, modifier: Modifier = Modifier) {
    val (filePath, kind) = remember(item) {
        when (item) {
            is DownloadListItem.Active -> item.progressInfo.videoInfo.thumbnail to
                if (item.progressInfo.videoInfo.isAudioOnly) MediaKind.AUDIO else MediaKind.VIDEO

            is DownloadListItem.Completed -> item.videoTaskItem.filePath to
                MediaKind.forFile(item.videoTaskItem.mimeType, item.videoTaskItem.fileName)
        }
    }

    val durationBadge = remember(item) { formatDurationBadge(item.durationMs) }

    Box(modifier = modifier.clip(ShapeMd)) {
        MediaThumbnail(
            filePath = filePath,
            mediaType = kind,
            modifier = Modifier.fillMaxSize(),
        )

        durationBadge?.let { duration ->
            Text(
                text = duration,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun CompletedStatus() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_status_done),
            contentDescription = null,
            modifier = Modifier.size(15.dp),
        )

        Text(
            text = stringResource(R.string.string_status_completed),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            fontWeight = FontWeight.SemiBold,
            color = Success,
            maxLines = 1,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun DownloadingStatus(info: ProgressInfo) {
    val isPaused = info.downloadStatus == VideoTaskState.PAUSE
    val accent = if (isPaused) Muted else Info

    Column {
        if (info.isFetchingInfo) {
            // No byte count exists yet (queued, or yt-dlp still extracting metadata) — a "0%"
            // bar here would read as stalled instead of as work in progress.
            Text(
                text = stringResource(
                    if (info.videoInfo.isAudioOnly) {
                        R.string.string_fetching_audio_info
                    } else {
                        R.string.string_fetching_video_info
                    },
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                color = Muted,
                maxLines = 1,
            )
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(
                    if (isPaused) R.drawable.ic_pause_glyph else R.drawable.ic_status_downloading,
                ),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )

            Text(
                text = if (isPaused) {
                    stringResource(R.string.string_download_paused_percent, info.progress.toString())
                } else {
                    stringResource(R.string.string_downloading_percent, info.progress.toString())
                },
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 5.dp),
            )

            Text(
                text = FileUtil.getFileSizeReadable(info.progressDownloaded.toDouble()) +
                    " / " + FileUtil.getFileSizeReadable(info.progressTotal.toDouble()),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp).weight(1f, fill = false),
            )
        }

        val animatedProgress by animateFloatAsState(
            targetValue = info.progress / 100f,
            animationSpec = tween(durationMillis = 400),
            label = "downloadProgress",
        )

        LinearProgressIndicator(
            progress = { animatedProgress },
            color = accent,
            trackColor = if (isPaused) Border else InfoSoft,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .padding(top = 5.dp)
                .fillMaxWidth()
                .height(5.dp)
                .clip(ShapePill),
        )
    }
}

@Composable
private fun FailedStatus(onRetry: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.string_download_failed_short),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            fontWeight = FontWeight.SemiBold,
            color = Error,
            maxLines = 1,
        )

        Icon(
            painter = painterResource(R.drawable.ic_reload),
            contentDescription = null,
            tint = PriInk,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(24.dp)
                .clip(ShapePill)
                .background(Error)
                .clickable(onClick = onRetry)
                .padding(5.dp),
        )
    }
}

@Composable
private fun WaitingForWifiStatus() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_wifi_off),
            contentDescription = null,
            tint = WarnInk,
            modifier = Modifier.size(14.dp),
        )

        Text(
            text = stringResource(R.string.string_waiting_for_wifi),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            fontWeight = FontWeight.SemiBold,
            color = WarnInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp).weight(1f, fill = false),
        )

        Text(
            text = stringResource(R.string.string_queued),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold,
            color = WarnInk,
            maxLines = 1,
            modifier = Modifier
                .padding(start = 6.dp)
                .clip(ShapePill)
                .background(Surface)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun RowActionButton(iconRes: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = TextColor,
        modifier = modifier
            .size(30.dp)
            .clip(ShapePill)
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(8.dp),
    )
}

@Composable
private fun DownloadGridTile(
    item: DownloadListItem,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .softShadow(ElevationCard, ShapeLg)
            .clip(ShapeLg)
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
            DownloadThumbnail(item, modifier = Modifier.fillMaxSize())

            if (selectionMode) {
                Box(modifier = Modifier.align(Alignment.TopStart).padding(5.dp)) {
                    SelectionDot(selected)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                DownloadMetaLine(item)
            }

            if (!selectionMode) {
                Icon(
                    painter = painterResource(R.drawable.ic_more),
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.padding(start = 4.dp).size(18.dp).clickable(onClick = onMenu),
                )
            }
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(ElevationFloatingBar, RectangleShape)
            .background(Surface)
            .padding(16.dp),
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
