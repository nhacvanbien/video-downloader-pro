package com.smarttool.videodownloader.feature.browser.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.ui.components.MediaKind
import com.smarttool.videodownloader.core.ui.components.MediaThumbnail
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.ElevationCard
import com.smarttool.videodownloader.core.ui.theme.ElevationControl
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriInk
import com.smarttool.videodownloader.core.ui.theme.PriSoft
import com.smarttool.videodownloader.core.ui.theme.SettingsOrange
import com.smarttool.videodownloader.core.ui.theme.SettingsOrangeSoft
import com.smarttool.videodownloader.core.ui.theme.ShapeLg
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Success
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.core.ui.theme.softShadow
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.feature.browser.domain.model.PRIMARY_SITE_COUNT
import com.smarttool.videodownloader.feature.browser.domain.model.PopularSite
import com.smarttool.videodownloader.feature.downloads.presentation.downloadFormatLabel
import com.smarttool.videodownloader.feature.downloads.presentation.downloadSourceLabel
import com.smarttool.videodownloader.feature.downloads.presentation.formatDurationBadge

private val PasteCardFill = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFFFFE7E4),
        0.45f to Color(0xFFFFF1EF),
        1.0f to Color(0xFFFFF8F7),
    ),
    start = Offset(0f, Float.POSITIVE_INFINITY),
    end = Offset(Float.POSITIVE_INFINITY, 0f),
)

private val DownloadButtonFill = Brush.linearGradient(listOf(Color(0xFFFF4A5C), Pri))

@Composable
fun BrowserHomeScreen(
    state: BrowserHomeContract.State,
    sites: List<PopularSite>,
    recentDownloads: List<VideoTaskItem>,
    onQueryChange: (String) -> Unit,
    onSubmitQuery: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    onOpenSite: (PopularSite) -> Unit,
    onOpenGuide: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMedia: (VideoTaskItem) -> Unit,
    onSeeAllDownloads: () -> Unit,
) {
    var sitesExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        BrowserHomeHeader(tabCount = state.tabCount, onOpenTabs = onOpenTabs)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            PasteLinkCard(
                query = state.query,
                onQueryChange = onQueryChange,
                onSubmit = onSubmitQuery,
                onPasteFromClipboard = onPasteFromClipboard,
                onOpenGuide = onOpenGuide,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShortcutCard(
                    iconRes = R.drawable.ic_clock_history,
                    iconBg = PriSoft,
                    iconTint = Pri,
                    titleRes = R.string.string_history,
                    subtitleRes = R.string.string_history_desc,
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f),
                )

                ShortcutCard(
                    iconRes = R.drawable.ic_bookmark,
                    iconBg = SettingsOrangeSoft,
                    iconTint = SettingsOrange,
                    titleRes = R.string.string_bookmark,
                    subtitleRes = R.string.string_bookmark_desc,
                    onClick = onOpenBookmarks,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionHeader(
                title = stringResource(R.string.string_popular_platforms),
                onSeeAll = { sitesExpanded = !sitesExpanded },
            )

            PopularSiteGrid(
                sites = sites,
                expanded = sitesExpanded,
                onOpenSite = onOpenSite,
                onToggleExpanded = { sitesExpanded = !sitesExpanded },
            )

            if (recentDownloads.isNotEmpty()) {
                SectionHeader(
                    title = stringResource(R.string.string_recent_videos),
                    onSeeAll = onSeeAllDownloads,
                )

                recentDownloads.forEach { item ->
                    RecentDownloadRow(
                        item = item,
                        onClick = { onOpenMedia(item) },
                        onMenu = onSeeAllDownloads,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun BrowserHomeHeader(tabCount: Int, onOpenTabs: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onOpenTabs),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_menu),
                contentDescription = null,
                tint = TextColor,
                modifier = Modifier.size(24.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.string_video_downloader),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                fontWeight = FontWeight.ExtraBold,
                color = TextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = stringResource(R.string.string_browser_home_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = Muted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onOpenTabs),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification),
                    contentDescription = null,
                    tint = TextColor,
                    modifier = Modifier.size(23.dp),
                )
            }

            if (tabCount > 0) {
                Text(
                    text = if (tabCount > 99) "99+" else tabCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = PriInk,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .offset(x = (-2).dp, y = 2.dp)
                        .defaultMinSize(minWidth = 17.dp)
                        .clip(CircleShape)
                        .background(Pri)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun PasteLinkCard(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    onOpenGuide: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeLg)
            .background(PasteCardFill),
    ) {
        HalftoneCorner(modifier = Modifier.align(Alignment.TopEnd))

        Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(PriSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_link),
                    contentDescription = null,
                    tint = Pri,
                    modifier = Modifier.size(23.dp),
                )
            }

            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = stringResource(R.string.string_paste_video_link),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                )

                Text(
                    text = stringResource(R.string.string_paste_link_supported_sites),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
                    color = Muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(ShapeMd)
                    .background(Surface)
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.string_paste_video_link_hint),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        cursorBrush = SolidColor(Pri),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = TextColor,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(PriSoft)
                        .clickable(onClick = onPasteFromClipboard),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_clipboard),
                        contentDescription = null,
                        tint = Pri,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .softShadow(ElevationCard, ShapePill)
                    .clip(ShapePill)
                    .background(DownloadButtonFill)
                    .clickable(onClick = onSubmit)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_download_line),
                    contentDescription = null,
                    tint = PriInk,
                    modifier = Modifier.size(19.dp),
                )

                Text(
                    text = stringResource(R.string.string_download_cta),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Bold,
                    color = PriInk,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 7.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PasteCardAction(
                iconRes = R.drawable.ic_clipboard,
                labelRes = R.string.string_paste_from_clipboard,
                onClick = onPasteFromClipboard,
            )

            PasteCardAction(
                iconRes = R.drawable.ic_help_circle,
                labelRes = R.string.string_guide,
                onClick = onOpenGuide,
            )
        }
        }
    }
}

/**
 * The halftone flourish in the card's top-right corner. Drawn rather than shipped as a vector so
 * it inherits the brand colour and cannot fall out of sync with it.
 */
@Composable
private fun HalftoneCorner(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 150.dp, height = 108.dp)) {
        val spacing = 11.dp.toPx()
        val radius = 1.7.dp.toPx()

        var y = spacing / 2f
        while (y < size.height) {
            var x = spacing / 2f
            while (x < size.width) {
                // Densest at the corner itself, dissolving towards the card's content.
                val towardsCorner = (x / size.width) * (1f - y / size.height)
                drawCircle(
                    color = Pri.copy(alpha = 0.05f + 0.22f * towardsCorner),
                    radius = radius,
                    center = Offset(x, y),
                )
                x += spacing
            }
            y += spacing
        }
    }
}

@Composable
private fun PasteCardAction(iconRes: Int, labelRes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(ShapePill)
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Pri,
            modifier = Modifier.size(16.dp),
        )

        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            fontWeight = FontWeight.SemiBold,
            color = TextColor,
            maxLines = 1,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

@Composable
private fun ShortcutCard(
    iconRes: Int,
    iconBg: Color,
    iconTint: Color,
    titleRes: Int,
    subtitleRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .softShadow(ElevationCard, ShapeLg)
            .clip(ShapeLg)
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(ShapeMd).background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp),
            )
        }

        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Bold,
                color = TextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = stringResource(subtitleRes),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_arrow_next),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
            fontWeight = FontWeight.Bold,
            color = TextColor,
            modifier = Modifier.weight(1f),
        )

        Row(
            modifier = Modifier.clip(ShapePill).clickable(onClick = onSeeAll).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.string_see_all),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold,
                color = Pri,
                maxLines = 1,
            )

            Icon(
                painter = painterResource(R.drawable.ic_arrow_next),
                contentDescription = null,
                tint = Pri,
                modifier = Modifier.padding(start = 2.dp).size(14.dp),
            )
        }
    }
}

/**
 * Laid out as plain rows rather than a lazy grid: the whole page already scrolls, and nesting a
 * vertically scrolling grid inside it would leave the grid with unbounded height.
 */
@Composable
private fun PopularSiteGrid(
    sites: List<PopularSite>,
    expanded: Boolean,
    onOpenSite: (PopularSite) -> Unit,
    onToggleExpanded: () -> Unit,
) {
    val shown = if (expanded) sites else sites.take(PRIMARY_SITE_COUNT)
    val cells = shown.size + if (expanded) 0 else 1
    val rows = (cells + 3) / 4

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(rows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { column ->
                    val index = row * 4 + column

                    when {
                        index < shown.size -> PopularSiteTile(
                            iconRes = shown[index].iconRes,
                            label = stringResource(shown[index].labelRes),
                            onClick = { onOpenSite(shown[index]) },
                            modifier = Modifier.weight(1f),
                        )

                        index == shown.size && !expanded -> PopularSiteTile(
                            iconRes = R.drawable.ic_more_dots,
                            label = stringResource(R.string.string_other),
                            onClick = onToggleExpanded,
                            modifier = Modifier.weight(1f),
                        )

                        else -> Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PopularSiteTile(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clip(ShapeMd).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .softShadow(ElevationControl, ShapeLg)
                .clip(ShapeLg)
                .background(Surface),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = TextColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

@Composable
private fun RecentDownloadRow(
    item: VideoTaskItem,
    onClick: () -> Unit,
    onMenu: () -> Unit,
) {
    val meta = remember(item) {
        listOfNotNull(
            downloadSourceLabel(item.url),
            item.fileSize.takeIf { it > 0 }?.let { FileUtil.getFileSizeReadable(it.toDouble()) },
            downloadFormatLabel(item.fileName),
        ).joinToString("  •  ")
    }

    Row(
        modifier = Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .softShadow(ElevationCard, ShapeLg)
            .clip(ShapeLg)
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(96.dp).height(62.dp).clip(ShapeMd)) {
            MediaThumbnail(
                filePath = item.filePath,
                mediaType = MediaKind.forFile(item.mimeType, item.fileName),
                modifier = Modifier.fillMaxSize(),
            )

            formatDurationBadge(item.fileDuration)?.let { duration ->
                Text(
                    text = duration,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
            Text(
                text = item.title.ifBlank { item.fileName },
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Bold,
                color = TextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 5.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_status_done),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
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
