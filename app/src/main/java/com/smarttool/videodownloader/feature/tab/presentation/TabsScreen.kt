package com.smarttool.videodownloader.feature.tab.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.ScreenBg
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.PriInk
import com.smarttool.videodownloader.core.ui.theme.PriSoft
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel

@Composable
fun TabsScreen(
    tabs: List<TabModel>,
    onOpenTab: (TabModel) -> Unit,
    onDeleteTab: (TabModel) -> Unit,
    onCloseAll: () -> Unit,
    onNewTab: () -> Unit,
    onHome: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_home),
                    colorFilter = ColorFilter.tint(TextColor),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp).clickable(onClick = onHome).padding(3.dp),
                )

                Text(
                    text = stringResource(R.string.string_num_tabs, tabs.size.toString()),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextColor,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )

                Text(
                    text = stringResource(R.string.string_close_all),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                    color = Pri,
                    modifier = Modifier
                        .clip(ShapePill)
                        .background(Surface)
                        .border(1.dp, Border, ShapePill)
                        .clickable(onClick = onCloseAll)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp).navigationBarsPadding(),
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

        FloatingActionButton(
            onClick = onNewTab,
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

@Composable
private fun TabCard(
    tab: TabModel,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val favicon = tab.faviconBitmap()

    Column(
        modifier = Modifier
            .clip(ShapeMd)
            .background(Surface)
            .border(1.dp, if (tab.isSelected) Pri else Border, ShapeMd)
            .clickable(onClick = onOpen)
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                color = TextColor,
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
                .padding(top = 8.dp)
                .clip(ShapeMd)
                .background(PriSoft),
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
