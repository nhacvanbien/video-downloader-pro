package com.smarttool.videodownloader.feature.browser.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.SheetCornerRadius
import com.smarttool.videodownloader.core.ui.theme.AppBlack
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.SearchFieldHint
import com.smarttool.videodownloader.core.ui.theme.TextPrimary
import com.smarttool.videodownloader.feature.browser.domain.model.PopularSite

@Composable
fun BrowserHomeScreen(
    state: BrowserHomeContract.State,
    sites: List<PopularSite>,
    onQueryChange: (String) -> Unit,
    onSubmitQuery: () -> Unit,
    onOpenSite: (PopularSite) -> Unit,
    onOpenGuide: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_guide),
                contentDescription = null,
                modifier = Modifier.size(28.dp).clickable(onClick = onOpenGuide),
            )

            Text(
                text = stringResource(R.string.string_video_downloader),
                style = MaterialTheme.typography.titleLarge,
                color = AppWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )

            Text(
                text = state.tabCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppWhite)
                    .clickable(onClick = onOpenTabs)
                    .padding(top = 4.dp),
            )
        }

        SearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            onSubmit = onSubmitQuery,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShortcutChip(
                iconRes = R.drawable.ic_bookmark,
                labelRes = R.string.string_bookmark,
                onClick = onOpenBookmarks,
                modifier = Modifier.weight(1f),
            )

            ShortcutChip(
                iconRes = R.drawable.ic_history,
                labelRes = R.string.string_history,
                onClick = onOpenHistory,
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius))
                .background(AppWhite),
        ) {
            Text(
                text = stringResource(R.string.string_popular_apps),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sites, key = { it.url }) { site ->
                    PopularSiteTile(site = site, onClick = { onOpenSite(site) })
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(120.dp))
            .background(AppWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Box(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.string_search_for_anything),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SearchFieldHint,
                )
            }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(Primary),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppBlack),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(20.dp).clickable(onClick = onSubmit),
        )

    }
}

@Composable
private fun ShortcutChip(
    iconRes: Int,
    labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(120.dp))
            .background(AppWhite)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )

        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
            color = TextPrimary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun PopularSiteTile(
    site: PopularSite,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AppWhite),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(site.iconRes),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )
        }

        Text(
            text = stringResource(site.labelRes),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
