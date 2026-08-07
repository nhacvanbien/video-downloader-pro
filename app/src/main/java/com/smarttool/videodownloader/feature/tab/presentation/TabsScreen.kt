package com.smarttool.videodownloader.feature.tab.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.ScreenGradient
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.TextPrimary
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel

private val SelectedTint = Color(0xFFF46621)
private val NormalTint = Color(0xFFF2F2F2)
private val SelectedBackground = Color(0xFFF6F2FE)

@Composable
fun TabsScreen(
    tabs: List<TabModel>,
    onOpenTab: (TabModel) -> Unit,
    onDeleteTab: (TabModel) -> Unit,
    onCloseAll: () -> Unit,
    onNewTab: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(ScreenGradient)) {
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_add),
                colorFilter = ColorFilter.tint(AppWhite),
                contentDescription = null,
                modifier = Modifier.size(28.dp).clickable(onClick = onNewTab).padding(4.dp),
            )

            Text(
                text = stringResource(R.string.string_num_tabs, tabs.size.toString()),
                style = MaterialTheme.typography.titleLarge,
                color = AppWhite,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )

            Text(
                text = stringResource(R.string.string_close_all),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                color = Primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(120.dp))
                    .background(AppWhite)
                    .clickable(onClick = onCloseAll)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(tabs, key = { it.id }) { tab ->
                TabCard(
                    tab = tab,
                    onOpen = { onOpenTab(tab) },
                    onDelete = { onDeleteTab(tab) },
                )
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: TabModel,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val favicon = tab.faviconBitmap()

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (tab.isSelected) SelectedTint else NormalTint)
            .clickable(onClick = onOpen)
            .padding(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (favicon != null) {
                Image(
                    bitmap = favicon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = tab.url,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            )

            Image(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier.size(16.dp).clickable(onClick = onDelete),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (tab.isSelected) SelectedBackground else NormalTint),
            contentAlignment = Alignment.Center,
        ) {
            if (favicon != null) {
                Image(
                    bitmap = favicon.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    }
}
