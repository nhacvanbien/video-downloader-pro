package com.smarttool.videodownloader.feature.language.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.LottieView
import com.smarttool.videodownloader.core.ui.components.ScreenBg
import com.smarttool.videodownloader.core.ui.theme.Bg
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriSoft
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.feature.language.domain.model.AppLanguage
import com.smarttool.videodownloader.feature.library.presentation.MediaSearchBar

private val NewUiBackground = Bg
private const val HINT_ANIMATION_INDEX = 3

@Composable
fun LanguageScreen(
    state: LanguageContract.State,
    mode: LanguageMode,
    showLoadingOverlay: Boolean,
    headerTitle: String,
    onSelect: (String) -> Unit,
    onSearch: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val hasSelection = state.selectedCode != null
    val backgroundModifier = if (mode == LanguageMode.FirstOpenNew) {
        Modifier.background(NewUiBackground)
    } else {
        Modifier.background(ScreenBg)
    }

    Box(modifier = Modifier.fillMaxSize().then(backgroundModifier)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBg)
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        ) {
            LanguageToolbar(
                mode = mode,
                headerTitle = headerTitle,
                showConfirm = mode != LanguageMode.FirstOpenNew || hasSelection,
                onConfirm = onConfirm,
                onBack = onBack,
            )

            MediaSearchBar(
                search = state.searchQuery,
                onSearchChange = onSearch,
                hint = stringResource(R.string.string_search_language),
            )

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(state.languages, key = { _, item -> item.code }) { index, language ->
                    LanguageRow(
                        language = language,
                        selected = language.code == state.selectedCode,
                        showHint = state.showHintAnimation &&
                            index == HINT_ANIMATION_INDEX &&
                            mode != LanguageMode.Settings,
                        onClick = { onSelect(language.code) },
                    )
                }
            }
        }

        if (showLoadingOverlay) {
            Column(
                modifier = Modifier.fillMaxSize().background(Surface),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LottieView(rawRes = R.raw.language, modifier = Modifier.size(77.dp))

                CircularProgressIndicator(
                    color = Pri,
                    strokeWidth = 4.dp,
                    modifier = Modifier.padding(top = 16.dp).size(35.dp),
                )
            }
        }
    }
}

@Composable
private fun LanguageToolbar(
    mode: LanguageMode,
    headerTitle: String,
    showConfirm: Boolean,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mode == LanguageMode.Settings) {
            Image(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = null,
                modifier = Modifier.size(28.dp).clickable(onClick = onBack).padding(4.dp),
            )
        }

        Text(
            text = headerTitle,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = if (mode == LanguageMode.Settings) 18.sp else 16.sp,
            ),
            color = TextColor,
            modifier = Modifier.weight(1f).padding(end = 5.dp),
        )

        if (!showConfirm) return@Row

        if (mode == LanguageMode.FirstOpenNew) {
            Image(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.clickable(onClick = onConfirm).padding(12.dp),
            )
        } else {
            Text(
                text = stringResource(R.string.string_save),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = Pri,
                modifier = Modifier
                    .clip(ShapePill)
                    .background(Surface)
                    .border(1.dp, Border, ShapePill)
                    .clickable(onClick = onConfirm)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    selected: Boolean,
    showHint: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeMd)
            .background(if (selected) PriSoft else Surface)
            .border(1.dp, if (selected) Pri else Border, ShapeMd)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(language.iconRes),
            contentDescription = null,
            modifier = Modifier.width(44.dp).height(28.dp),
        )

        Text(
            text = language.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (selected) Pri else TextColor,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )

        if (showHint) {
            LottieView(
                rawRes = R.raw.tap_android,
                modifier = Modifier.size(30.dp),
            )
        }

        if (selected) {
            Image(
                painter = painterResource(R.drawable.ic_check_box_selected),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
