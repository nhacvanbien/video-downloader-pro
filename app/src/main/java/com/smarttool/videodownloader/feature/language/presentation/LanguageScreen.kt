package com.smarttool.videodownloader.feature.language.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.LottieView
import com.smarttool.videodownloader.core.ui.components.NativeAdFullContainer
import com.smarttool.videodownloader.core.ui.components.ScreenGradient
import com.smarttool.videodownloader.core.ui.theme.AppBlack
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.TextPrimary
import com.smarttool.videodownloader.feature.language.domain.model.AppLanguage
import com.smarttool.videodownloader.core.ads.AdsConstant

private val NewUiBackground = Color(0xFFF3F3F3)
private const val HINT_ANIMATION_INDEX = 3

@Composable
fun LanguageScreen(
    state: LanguageUiState,
    mode: LanguageMode,
    showLoadingOverlay: Boolean,
    headerTitle: String,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val hasSelection = state.selectedCode != null
    val backgroundModifier = if (mode == LanguageMode.FirstOpenNew) {
        Modifier.background(NewUiBackground)
    } else {
        Modifier.background(ScreenGradient)
    }

    Box(modifier = Modifier.fillMaxSize().then(backgroundModifier)) {
        Column(modifier = Modifier.fillMaxSize()) {
            LanguageToolbar(
                mode = mode,
                headerTitle = headerTitle,
                showConfirm = mode != LanguageMode.FirstOpenNew || hasSelection,
                onConfirm = onConfirm,
                onBack = onBack,
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

            // The second ad slot replaces the first once a language is picked, mirroring
            // the preload-then-swap behaviour the onboarding funnel depends on.
            if (mode != LanguageMode.Settings && hasSelection) {
                NativeAdFullContainer(
                    adUnitId = BuildConfig.NATIVE_LANGUAGE_1_2,
                    layoutRes = R.layout.layout_native_language_2,
                    adPlacement = "native_language_1_2",
                    canShowAds = AdsConstant.showNativeLanguage1_2,
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                )
            } else {
                NativeAdFullContainer(
                    adUnitId = BuildConfig.NATIVE_LANGUAGE_1_1,
                    layoutRes = R.layout.layout_native_language,
                    adPlacement = "native_language_1_1",
                    canShowAds = AdsConstant.showNativeLanguage1_1,
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                )
            }
        }

        if (showLoadingOverlay) {
            Column(
                modifier = Modifier.fillMaxSize().background(AppWhite),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LottieView(rawRes = R.raw.language, modifier = Modifier.size(77.dp))

                CircularProgressIndicator(
                    color = Primary,
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
            color = if (mode == LanguageMode.FirstOpenNew) AppBlack else AppWhite,
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
                color = Primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(120.dp))
                    .background(AppWhite)
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
            .clip(RoundedCornerShape(12.dp))
            .background(AppWhite)
            .then(
                if (selected) {
                    Modifier.border(1.dp, Primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
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
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
            color = TextPrimary,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )

        if (showHint) {
            LottieView(
                rawRes = R.raw.tap_android,
                modifier = Modifier.size(30.dp),
            )
        }

        Image(
            painter = painterResource(
                if (selected) R.drawable.ic_dot_selected else R.drawable.ic_dot_normal,
            ),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
    }
}
