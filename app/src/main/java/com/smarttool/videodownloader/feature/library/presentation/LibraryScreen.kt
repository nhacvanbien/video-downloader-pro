package com.smarttool.videodownloader.feature.library.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.ui.components.MediaKind
import com.smarttool.videodownloader.core.ui.components.MediaThumbnail
import com.smarttool.videodownloader.core.ui.components.SheetCornerRadius
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Error
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.core.ui.theme.TextPrimary
import com.smarttool.videodownloader.core.ui.theme.TextSub
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.feature.library.domain.model.MediaFilter

@Composable
fun LibraryScreen(
    title: String,
    state: LibraryContract.State,
    items: List<VideoTaskItem>,
    onFilterChange: (MediaFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenSort: () -> Unit,
    onItemClick: (VideoTaskItem) -> Unit,
    onItemMenu: (VideoTaskItem) -> Unit,
    onToggleSelection: (String) -> Unit,
    onItemLongClick: (VideoTaskItem) -> Unit = {},
    onBack: (() -> Unit)? = null,
    trailingAction: @Composable (() -> Unit)? = null,
    selectionActions: SelectionActions? = null,
) {
    // Nested inside MainScreen's Downloaded tab, `onBack` is null and MainScreen already
    // insets the tab content; standalone (private area / select-video), it's a top-level
    // route that must reserve the system bars itself.
    val insetModifier = if (onBack != null) {
        Modifier.statusBarsPadding().navigationBarsPadding()
    } else {
        Modifier
    }

    Column(modifier = Modifier.fillMaxSize().then(insetModifier)) {
        LibraryToolbar(
            title = title,
            onBack = onBack,
            trailingAction = trailingAction,
        )

        if (state.selectionMode && selectionActions != null) {
            SelectionBar(
                selectedCount = state.selectedIds.size,
                actions = selectionActions,
            )
        }

        MediaFilterChipRow(selected = state.query.filter, onSelect = onFilterChange)

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius))
                .background(Surface),
        ) {
            MediaSearchBar(
                search = state.query.search,
                onSearchChange = onSearchChange,
                hint = stringResource(R.string.string_enter_search_name),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.string_num_files, items.size.toString()),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )

                Image(
                    painter = painterResource(R.drawable.ic_sort),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp).clickable(onClick = onOpenSort),
                )
            }

            if (items.isEmpty()) {
                EmptyLibrary()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 4.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.mId }) { item ->
                        MediaRow(
                            item = item,
                            selectionMode = state.selectionMode,
                            selected = item.mId in state.selectedIds,
                            onClick = {
                                if (state.selectionMode) {
                                    onToggleSelection(item.mId)
                                } else {
                                    onItemClick(item)
                                }
                            },
                            onMenu = { onItemMenu(item) },
                            onLongClick = { onItemLongClick(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryToolbar(
    title: String,
    onBack: (() -> Unit)?,
    trailingAction: @Composable (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Image(
                painter = painterResource(R.drawable.ic_arrow_back),
                colorFilter = ColorFilter.tint(TextColor),
                contentDescription = null,
                modifier = Modifier.size(28.dp).clickable(onClick = onBack).padding(4.dp),
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )

        trailingAction?.let {
            Box(modifier = Modifier.padding(start = 12.dp)) { it() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaRow(
    item: VideoTaskItem,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Image(
                painter = painterResource(
                    if (selected) R.drawable.ic_dot_selected else R.drawable.ic_dot_normal,
                ),
                contentDescription = null,
                modifier = Modifier.size(22.dp).padding(end = 8.dp),
            )
        }

        MediaThumbnail(
            filePath = item.filePath,
            mediaType = MediaKind.fromMimeType(item.mimeType),
            modifier = Modifier
                .size(width = 64.dp, height = 48.dp)
                .clip(RoundedCornerShape(8.dp)),
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                text = item.title.ifBlank { item.fileName },
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = FileUtil.getFileSizeReadable(item.fileSize.toDouble()),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = TextSub,
            )
        }

        Image(
            painter = painterResource(R.drawable.ic_more),
            contentDescription = null,
            modifier = Modifier.size(22.dp).clickable(onClick = onMenu),
        )
    }
}

@Composable
private fun EmptyLibrary() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_airboat),
            contentDescription = null,
        )

        Text(
            text = stringResource(R.string.string_no_files_have_been_downloaded_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSub,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 40.dp, end = 40.dp),
        )
    }
}


@Composable
private fun SelectionBar(
    selectedCount: Int,
    actions: SelectionActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_close),
            colorFilter = ColorFilter.tint(TextColor),
            contentDescription = null,
            modifier = Modifier.size(20.dp).clickable(onClick = actions.onCancel),
        )

        Text(
            text = stringResource(R.string.string_num_video_selected, selectedCount.toString()),
            style = MaterialTheme.typography.labelLarge,
            color = TextColor,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )

        SelectionBarAction(
            iconRes = R.drawable.ic_selected,
            onClick = actions.onSelectAll,
        )

        SelectionBarAction(
            iconRes = actions.privateActionIconRes,
            onClick = actions.onTogglePrivate,
            modifier = Modifier.padding(start = 10.dp),
        )

        SelectionBarAction(
            iconRes = R.drawable.ic_delete,
            onClick = actions.onDelete,
            background = Error,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun SelectionBarAction(
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: androidx.compose.ui.graphics.Color = Surface,
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(ShapePill)
            .background(background)
            .let { if (background == Surface) it.border(1.dp, Border, ShapePill) else it }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            colorFilter = if (background == Surface) ColorFilter.tint(TextColor) else null,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}
