package com.smarttool.videodownloader.feature.settings.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriSoft
import com.smarttool.videodownloader.core.ui.theme.SettingsBlue
import com.smarttool.videodownloader.core.ui.theme.SettingsBlueSoft
import com.smarttool.videodownloader.core.ui.theme.SettingsGold
import com.smarttool.videodownloader.core.ui.theme.SettingsGoldSoft
import com.smarttool.videodownloader.core.ui.theme.SettingsGrayIcon
import com.smarttool.videodownloader.core.ui.theme.SettingsGraySoft
import com.smarttool.videodownloader.core.ui.theme.SettingsOrange
import com.smarttool.videodownloader.core.ui.theme.SettingsOrangeSoft
import com.smarttool.videodownloader.core.ui.theme.SettingsPurple
import com.smarttool.videodownloader.core.ui.theme.SettingsPurpleSoft
import com.smarttool.videodownloader.core.ui.theme.ElevationCard
import com.smarttool.videodownloader.core.ui.theme.ElevationControl
import com.smarttool.videodownloader.core.ui.theme.ShapeLg
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.softShadow
import com.smarttool.videodownloader.core.ui.theme.Success
import com.smarttool.videodownloader.core.ui.theme.SuccessTint
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RowTitleColor = TextColor
private val RowSubtitleColor = Muted

// Row metrics — the divider inset must line up with where the row's text column starts.
private val RowHorizontalPadding = 14.dp
private val RowIconBadgeSize = 40.dp
private val RowIconGap = 12.dp
private val RowDividerInset = RowHorizontalPadding + RowIconBadgeSize + RowIconGap
private val RowMinHeight = 72.dp

/**
 * A hairline between two rows of the same card only has to hint at the split — at full [Border]
 * strength it competes with the card's own edge and chops the group into stripes.
 */
private val RowDividerColor = Border.copy(alpha = 0.4f)

/**
 * The app card is the one tinted surface on the page: a diagonal wash that starts on brand pink
 * at the top-left and gives way to plain [Surface], edged by a stroke that fades along the same
 * diagonal so the tint reads as lit from that corner rather than boxed in.
 */
private val AppInfoCardFill = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFFFFE6E2),
        0.30f to Color(0xFFFFF4F2),
        0.62f to Color(0xFFFFFCFB),
        1.0f to Surface,
    ),
    start = Offset(0f, Float.POSITIVE_INFINITY),
    end = Offset(Float.POSITIVE_INFINITY, 0f),
)

private val AppInfoCardStroke = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Pri.copy(alpha = 0.16f),
        0.45f to Pri.copy(alpha = 0.05f),
        1.0f to Color.Transparent,
    ),
    start = Offset(0f, Float.POSITIVE_INFINITY),
    end = Offset(Float.POSITIVE_INFINITY, 0f),
)

private fun searchEngineLabelRes(engine: SearchEngine): Int = when (engine) {
    SearchEngine.GOOGLE -> R.string.string_google
    SearchEngine.BING -> R.string.string_bing
    SearchEngine.YAHOO -> R.string.string_yahoo
    SearchEngine.DUCK_DUCK_GO -> R.string.string_duckduckgo
}

@Composable
fun SettingsScreen(
    state: SettingsContract.State,
    onLanguageClick: () -> Unit,
    onWifiOnlyToggle: () -> Unit,
    onSearchEngineClick: () -> Unit,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    onPolicyClick: () -> Unit,
    onDownloadLocationClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsHeader()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            AppInfoCard(state)

            SettingsSectionHeader(stringResource(R.string.string_downloads))

            SettingsGroup {
                SettingsRow(
                    iconRes = R.drawable.ic_folder,
                    iconBg = PriSoft,
                    iconTint = Pri,
                    title = stringResource(R.string.string_download_location),
                    subtitle = File(state.downloadLocation).name,
                    subtitleSecondary = state.downloadLocation,
                    showChevron = true,
                    onClick = onDownloadLocationClick,
                )

                SettingsRowDivider()

                SettingsToggleRow(
                    iconRes = R.drawable.ic_wifi_only,
                    iconBg = SuccessTint,
                    iconTint = Success,
                    title = stringResource(R.string.string_wifi_only),
                    subtitle = stringResource(R.string.string_wifi_only_desc),
                    checked = state.wifiOnly,
                    onToggle = onWifiOnlyToggle,
                )

                SettingsRowDivider()

                SettingsStorageRow(state)
            }

            SettingsSectionHeader(stringResource(R.string.string_settings_section_app))

            SettingsGroup {
                SettingsRow(
                    iconRes = R.drawable.ic_languages,
                    iconBg = SettingsBlueSoft,
                    iconTint = SettingsBlue,
                    title = stringResource(R.string.string_languages),
                    showChevron = true,
                    onClick = onLanguageClick,
                )

                SettingsRowDivider()

                SettingsRow(
                    iconRes = R.drawable.ic_search,
                    iconBg = SettingsOrangeSoft,
                    iconTint = SettingsOrange,
                    title = stringResource(R.string.string_search_engine),
                    subtitle = stringResource(searchEngineLabelRes(state.searchEngine)),
                    showChevron = true,
                    onClick = onSearchEngineClick,
                )
            }

            SettingsSectionHeader(stringResource(R.string.string_settings_section_other))

            SettingsGroup {
                SettingsRow(
                    iconRes = R.drawable.ic_share,
                    iconBg = PriSoft,
                    iconTint = Pri,
                    title = stringResource(R.string.string_settings_share_title),
                    subtitle = stringResource(R.string.string_settings_share_desc),
                    showChevron = true,
                    onClick = onShareClick,
                )

                if (state.showRateRow) {
                    SettingsRowDivider()

                    SettingsRow(
                        iconRes = R.drawable.ic_rate,
                        iconBg = SettingsGoldSoft,
                        iconTint = SettingsGold,
                        title = stringResource(R.string.string_settings_rate_title),
                        subtitle = stringResource(R.string.string_settings_rate_desc),
                        showChevron = true,
                        onClick = onRateClick,
                    )
                }

                SettingsRowDivider()

                SettingsRow(
                    iconRes = R.drawable.ic_policy,
                    iconBg = SuccessTint,
                    iconTint = Success,
                    title = stringResource(R.string.string_privacy_policy),
                    subtitle = stringResource(R.string.string_settings_policy_desc),
                    showChevron = true,
                    onClick = onPolicyClick,
                )

                SettingsRowDivider()

                SettingsRow(
                    iconRes = R.drawable.ic_about,
                    iconBg = SettingsGraySoft,
                    iconTint = SettingsGrayIcon,
                    title = stringResource(R.string.string_settings_about),
                    subtitle = stringResource(R.string.string_settings_version_label, state.versionName),
                    showChevron = true,
                    onClick = null,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.string_settings),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
            fontWeight = FontWeight.ExtraBold,
            color = RowTitleColor,
            modifier = Modifier.weight(1f),
        )

        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .softShadow(ElevationControl, CircleShape)
                    .clip(CircleShape)
                    .background(Surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification),
                    contentDescription = null,
                    tint = RowTitleColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            Box(
                modifier = Modifier
                    .offset(x = 1.dp, y = (-1).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Pri),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "3",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun AppInfoCard(state: SettingsContract.State) {
    val dateFormatter = remember(Locale.getDefault()) { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeLg)
            .background(AppInfoCardFill)
            .border(1.dp, AppInfoCardStroke, ShapeLg)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Pri, Color(0xFFFF6A3D)))),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_download_arrow),
                    contentDescription = null,
                    modifier = Modifier.size(27.dp),
                )
            }

            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.string_video_downloader),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = RowTitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(PriSoft)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.string_settings_pro_badge),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = Pri,
                            maxLines = 1,
                        )
                    }
                }

                val updatedText = if (state.lastUpdateTimeMillis > 0) {
                    " • " + stringResource(
                        R.string.string_settings_updated_label,
                        dateFormatter.format(Date(state.lastUpdateTimeMillis)),
                    )
                } else {
                    ""
                }

                Text(
                    text = stringResource(R.string.string_settings_version_label, state.versionName) + updatedText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = RowSubtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    SettingsStatItem(
                        modifier = Modifier.weight(1f),
                        iconRes = R.drawable.ic_stat_play,
                        iconBg = Pri,
                        value = state.videoCount.toString(),
                        label = stringResource(R.string.string_settings_stat_videos),
                    )

                    StatDivider()

                    SettingsStatItem(
                        modifier = Modifier.weight(1f),
                        iconRes = R.drawable.ic_stat_pie,
                        iconBg = SettingsPurple,
                        value = compactSize(state.usedBytes),
                        label = stringResource(R.string.string_settings_stat_total_size),
                    )

                    StatDivider()

                    SettingsStatItem(
                        modifier = Modifier.weight(1f),
                        iconRes = R.drawable.ic_stat_check,
                        iconBg = Success,
                        value = state.activeDownloadCount.toString(),
                        label = stringResource(R.string.string_settings_stat_downloading),
                    )
                }
            }
        }
    }
}

/**
 * The stat cells share the narrow column beside the app icon, where the two decimals
 * [FileUtil.getFileSizeReadable] emits ("425.22 MB") do not fit.
 */
private fun compactSize(bytes: Long): String =
    FileUtil.getFileSizeReadable(bytes.toDouble()).replace(Regex("(\\.\\d)\\d+"), "$1")

@Composable
private fun SettingsStatItem(
    iconRes: Int,
    iconBg: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(StatBadgeSize).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(15.dp),
            )
        }

        Column(modifier = Modifier.weight(1f).padding(start = 5.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.5.sp, lineHeight = 15.sp),
                fontWeight = FontWeight.Bold,
                color = RowTitleColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 10.5.sp),
                color = RowSubtitleColor,
                minLines = 2,
                maxLines = 2,
            )
        }
    }
}

private val StatBadgeSize = 28.dp

/** Hairline between two stats, matching the reference card's split into three equal cells. */
@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .width(1.dp)
            .height(30.dp)
            .background(Color(0xFFF3D9D6)),
    )
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, letterSpacing = 0.8.sp),
        fontWeight = FontWeight.SemiBold,
        color = RowSubtitleColor,
        modifier = Modifier.padding(top = 20.dp, start = 4.dp, bottom = 8.dp),
    )
}

/** All rows of one section share a single rounded card; rows are separated by [SettingsRowDivider]. */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(ElevationCard, ShapeMd)
            .clip(ShapeMd)
            .background(Surface),
        content = content,
    )
}

@Composable
private fun SettingsRowDivider() {
    Box(
        modifier = Modifier
            .padding(start = RowDividerInset)
            .fillMaxWidth()
            .height(1.dp)
            .background(RowDividerColor),
    )
}

@Composable
private fun SettingsIconBadge(iconRes: Int, bg: Color, tint: Color) {
    Box(
        modifier = Modifier.size(RowIconBadgeSize).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsRowChevron(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_arrow_next),
        contentDescription = null,
        tint = Muted,
        modifier = modifier.size(20.dp),
    )
}

@Composable
private fun SettingsRow(
    iconRes: Int,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    subtitleSecondary: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = RowHorizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(iconRes, iconBg, iconTint)

        Column(modifier = Modifier.weight(1f).padding(start = RowIconGap)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                color = RowTitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = RowSubtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            if (subtitleSecondary != null) {
                Text(
                    text = subtitleSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = RowSubtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }

        if (showChevron) {
            SettingsRowChevron(modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun SettingsToggleRow(
    iconRes: Int,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .clickable(onClick = onToggle)
            .padding(horizontal = RowHorizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(iconRes, iconBg, iconTint)

        Column(modifier = Modifier.weight(1f).padding(start = RowIconGap)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                color = RowTitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = RowSubtitleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Icon(
            painter = painterResource(if (checked) R.drawable.ic_switch_on else R.drawable.ic_switch_off),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.padding(start = 8.dp).width(44.dp).height(24.dp),
        )
    }
}

@Composable
private fun SettingsStorageRow(state: SettingsContract.State) {
    val usedFormatted = FileUtil.getFileSizeReadable(state.usedBytes.toDouble())
    val freeFormatted = FileUtil.getFileSizeReadable(state.freeBytes.toDouble())
    val total = (state.usedBytes + state.freeBytes).coerceAtLeast(1L)
    // Clamp only so a near-zero (but non-empty) library still paints a visible sliver.
    val usedFraction = (state.usedBytes.toFloat() / total.toFloat()).coerceIn(0.02f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .padding(horizontal = RowHorizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(R.drawable.ic_hard_drive, SettingsPurpleSoft, SettingsPurple)

        Column(modifier = Modifier.weight(1f).padding(start = RowIconGap)) {
            Text(
                text = stringResource(R.string.string_storage),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                color = RowTitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = stringResource(R.string.string_settings_storage_usage, usedFormatted, freeFormatted),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = RowSubtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )

            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SettingsPurpleSoft),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(usedFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(SettingsPurple),
                )
            }
        }

        SettingsRowChevron(modifier = Modifier.padding(start = 8.dp))
    }
}
