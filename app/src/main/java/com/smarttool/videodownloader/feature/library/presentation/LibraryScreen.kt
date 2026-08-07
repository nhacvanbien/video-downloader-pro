package com.smarttool.videodownloader.feature.library.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.MediaThumbnail
import com.smarttool.videodownloader.core.ui.components.NativeAdContainer
import com.smarttool.videodownloader.core.ui.components.ScreenGradient
import com.smarttool.videodownloader.core.ui.components.SheetCornerRadius
import com.smarttool.videodownloader.core.ui.theme.AppBlack
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.SearchFieldHint
import com.smarttool.videodownloader.core.ui.theme.TextPrimary
import com.smarttool.videodownloader.core.ui.theme.TextSub
import com.smarttool.videodownloader.feature.library.domain.model.MediaFilter
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem

@Composable
fun LibraryScreen(
    title: String,
    state: LibraryUiState,
    items: List<VideoTaskItem>,
    showAd: Boolean,
    onFilterChange: (MediaFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchVisibleChange: (Boolean) -> Unit,
    onOpenSort: () -> Unit,
    onItemClick: (VideoTaskItem) -> Unit,
    onItemMenu: (VideoTaskItem) -> Unit,
    onToggleSelection: (String) -> Unit,
    onItemLongClick: (VideoTaskItem) -> Unit = {},
    onBack: (() -> Unit)? = null,
    trailingAction: @Composable (() -> Unit)? = null,
    selectionActions: SelectionActions? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LibraryToolbar(
            title = title,
            state = state,
            onSearchChange = onSearchChange,
            onSearchVisibleChange = onSearchVisibleChange,
            onBack = onBack,
            trailingAction = trailingAction,
        )

        if (state.selectionMode && selectionActions != null) {
            SelectionBar(
                selectedCount = state.selectedIds.size,
                actions = selectionActions,
            )
        }

        MediaFilterTabs(selected = state.query.filter, onSelect = onFilterChange)

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius))
                .background(AppWhite),
        ) {
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

        if (showAd) {
            NativeAdContainer(
                adUnitId = BuildConfig.NATIVE_SMALL_ALL,
                layoutRes = R.layout.layout_native_ad_small_bottom,
                adPlacement = "native_downloaded",
                canShowAds = AdsConstant.showNativeSmallAll,
            )
        }
    }
}

@Composable
private fun LibraryToolbar(
    title: String,
    state: LibraryUiState,
    onSearchChange: (String) -> Unit,
    onSearchVisibleChange: (Boolean) -> Unit,
    onBack: (() -> Unit)?,
    trailingAction: @Composable (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.searchVisible) {
            Image(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onSearchVisibleChange(false) }
                    .padding(4.dp),
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(120.dp))
                    .background(AppWhite)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (state.query.search.isEmpty()) {
                        Text(
                            text = stringResource(R.string.string_enter_search_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SearchFieldHint,
                        )
                    }

                    BasicTextField(
                        value = state.query.search,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        cursorBrush = SolidColor(Primary),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppBlack),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            if (onBack != null) {
                Image(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp).clickable(onClick = onBack).padding(4.dp),
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = AppWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )

            Image(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onSearchVisibleChange(true) },
            )

            trailingAction?.let {
                Box(modifier = Modifier.padding(start = 12.dp)) { it() }
            }
        }
    }
}

@Composable
private fun MediaFilterTabs(
    selected: MediaFilter,
    onSelect: (MediaFilter) -> Unit,
) {
    val tabs = listOf(
        MediaFilter.All to R.string.string_all,
        MediaFilter.Video to R.string.string_video,
        MediaFilter.Image to R.string.string_image,
    )

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        tabs.forEach { (filter, labelRes) ->
            val active = filter == selected

            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                ),
                color = AppWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(filter) }
                    .padding(vertical = 10.dp),
            )
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
            isImage = item.mimeType.startsWith("image"),
            modifier = Modifier
                .size(width = 64.dp, height = 48.dp)
                .clip(RoundedCornerShape(8.dp)),
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                text = item.fileName,
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
            contentDescription = null,
            modifier = Modifier.size(20.dp).clickable(onClick = actions.onCancel),
        )

        Text(
            text = stringResource(R.string.string_num_video_selected, selectedCount.toString()),
            style = MaterialTheme.typography.labelLarge,
            color = AppWhite,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )

        Image(
            painter = painterResource(R.drawable.ic_selected),
            contentDescription = null,
            modifier = Modifier.size(22.dp).clickable(onClick = actions.onSelectAll),
        )

        Image(
            painter = painterResource(actions.privateActionIconRes),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 16.dp)
                .size(22.dp)
                .clickable(onClick = actions.onTogglePrivate),
        )

        Image(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 16.dp)
                .size(22.dp)
                .clickable(onClick = actions.onDelete),
        )
    }
}
