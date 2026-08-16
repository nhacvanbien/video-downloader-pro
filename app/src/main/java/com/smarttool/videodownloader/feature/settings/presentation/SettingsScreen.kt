package com.smarttool.videodownloader.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.AppTopBar
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine

private val RowTitleColor = TextColor
private val RowSubtitleColor = Muted

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
        AppTopBar(title = stringResource(R.string.string_settings))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
        ) {
            SettingsRow(
                iconRes = R.drawable.ic_download_location,
                title = stringResource(R.string.string_download_location),
                subtitle = state.downloadLocation,
                trailingIconRes = R.drawable.ic_edit,
                onClick = onDownloadLocationClick,
            )

            SettingsToggleRow(
                iconRes = R.drawable.ic_wifi_only,
                title = stringResource(R.string.string_wifi_only),
                subtitle = stringResource(R.string.string_wifi_only_desc),
                checked = state.wifiOnly,
                onToggle = onWifiOnlyToggle,
            )

            SettingsRow(
                iconRes = R.drawable.ic_languages,
                title = stringResource(R.string.string_languages),
                showChevron = true,
                onClick = onLanguageClick,
            )

            SettingsRow(
                iconRes = R.drawable.ic_search,
                title = stringResource(R.string.string_search_engine),
                subtitle = stringResource(searchEngineLabelRes(state.searchEngine)),
                showChevron = true,
                onClick = onSearchEngineClick,
            )

            if (state.showRateRow) {
                SettingsRow(
                    iconRes = R.drawable.ic_rate,
                    title = stringResource(R.string.string_rate),
                    showChevron = true,
                    onClick = onRateClick,
                )
            }

            SettingsRow(
                iconRes = R.drawable.ic_share,
                title = stringResource(R.string.string_share),
                showChevron = true,
                onClick = onShareClick,
            )

            SettingsRow(
                iconRes = R.drawable.ic_policy,
                title = stringResource(R.string.string_privacy_policy),
                showChevron = true,
                onClick = onPolicyClick,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    showChevron: Boolean = false,
    trailingIconRes: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(ShapeMd)
            .background(Surface)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp),
        )

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = RowTitleColor,
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = RowSubtitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }

        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_next),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp),
            )
        } else if (trailingIconRes != null) {
            Icon(
                painter = painterResource(trailingIconRes),
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(ShapeMd)
            .background(Surface)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp),
        )

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = RowTitleColor,
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = RowSubtitleColor,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        Icon(
            painter = painterResource(if (checked) R.drawable.ic_switch_on else R.drawable.ic_switch_off),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(width = 44.dp, height = 24.dp),
        )
    }
}
