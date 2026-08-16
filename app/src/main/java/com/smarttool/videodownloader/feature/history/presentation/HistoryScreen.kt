package com.smarttool.videodownloader.feature.history.presentation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.ScreenBg
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Error
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.PriInk
import com.smarttool.videodownloader.core.ui.theme.SearchFieldHint
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.core.ui.theme.TextPrimary
import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry

@Composable
fun HistoryScreen(
    state: HistoryContract.State,
    onBack: () -> Unit,
    onModeChange: (HistoryMode) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onEntryClick: (HistoryEntry) -> Unit,
    onDeleteEntry: (HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onAddBookmark: () -> Unit,
) {
    val isBookmarkMode = state.mode == HistoryMode.BOOKMARK

    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            HistoryTopBar(
                state = state,
                onBack = onBack,
                onSearchQueryChange = onSearchQueryChange,
                onSearchActiveChange = onSearchActiveChange,
            )

            if (!state.isSearchActive) {
                ModeSwitch(mode = state.mode, onModeChange = onModeChange)
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.entries.isEmpty()) {
                    EmptyState(isBookmarkMode = isBookmarkMode)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Surface),
                    ) {
                        LazyColumn(
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 10.dp),
                        ) {
                            items(state.entries, key = { it.id }) { entry ->
                                HistoryRow(
                                    entry = entry,
                                    onClick = { onEntryClick(entry) },
                                    onDelete = { onDeleteEntry(entry) },
                                )
                            }
                        }

                        if (!isBookmarkMode) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                            Text(
                                text = stringResource(R.string.clear_history),
                                style = MaterialTheme.typography.labelLarge,
                                color = Error,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(120.dp))
                                    .clickable(onClick = onClearHistory)
                                    .padding(vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }

                if (isBookmarkMode) {
                    FloatingActionButton(
                        onClick = onAddBookmark,
                        containerColor = Primary,
                        contentColor = PriInk,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(20.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTopBar(
    state: HistoryContract.State,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = null,
            tint = TextColor,
            modifier = Modifier
                .size(26.dp)
                .clickable { if (state.isSearchActive) onSearchActiveChange(false) else onBack() }
                .padding(4.dp),
        )

        if (state.isSearchActive) {
            SearchField(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChange,
                onClear = { onSearchQueryChange("") },
                modifier = Modifier.weight(1f).padding(horizontal = 7.dp, vertical = 9.dp),
            )
        } else {
            Text(
                text = stringResource(
                    if (state.mode == HistoryMode.BOOKMARK) {
                        R.string.string_bookmark
                    } else {
                        R.string.string_history
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                color = TextColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )

            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = TextColor,
                modifier = Modifier.size(20.dp).clickable { onSearchActiveChange(true) },
            )
        }
    }
}

@Composable
private fun ModeSwitch(mode: HistoryMode, onModeChange: (HistoryMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ModeSwitchTab(
            labelRes = R.string.string_history,
            selected = mode == HistoryMode.HISTORY,
            onClick = { onModeChange(HistoryMode.HISTORY) },
            modifier = Modifier.weight(1f),
        )
        ModeSwitchTab(
            labelRes = R.string.string_bookmark,
            selected = mode == HistoryMode.BOOKMARK,
            onClick = { onModeChange(HistoryMode.BOOKMARK) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModeSwitchTab(
    labelRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        ),
        color = if (selected) PriInk else Muted,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier
            .clip(ShapeMd)
            .background(if (selected) Primary else Surface)
            .border(1.dp, if (selected) Primary else Border, ShapeMd)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(120.dp))
            .background(Surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = SearchFieldHint,
            modifier = Modifier.size(20.dp),
        )

        Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.string_enter_search_name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SearchFieldHint,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(Primary),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null,
            tint = SearchFieldHint,
            modifier = Modifier.size(24.dp).clickable(onClick = onClear).padding(5.dp),
        )
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val favicon = remember(entry.favicon) {
        entry.favicon?.takeIf { it.isNotEmpty() }?.let {
            runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (favicon != null) {
            Image(
                bitmap = favicon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_placeholder_icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.url,
                style = MaterialTheme.typography.bodyMedium,
                color = SearchFieldHint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null,
            tint = SearchFieldHint,
            modifier = Modifier.size(24.dp).clickable(onClick = onDelete).padding(5.dp),
        )
    }
}

@Composable
private fun EmptyState(isBookmarkMode: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painter = painterResource(R.drawable.ic_airboat), contentDescription = null)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                if (isBookmarkMode) {
                    R.string.string_you_have_not_added
                } else {
                    R.string.string_go_see_the_sites
                },
            ),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
