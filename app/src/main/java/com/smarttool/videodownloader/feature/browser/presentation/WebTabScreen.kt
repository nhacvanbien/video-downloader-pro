package com.smarttool.videodownloader.feature.browser.presentation

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.RetainedAndroidView
import com.smarttool.videodownloader.core.ui.theme.AppBlack
import com.smarttool.videodownloader.core.ui.theme.AppGray
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.SearchFieldHint
import com.smarttool.videodownloader.feature.downloads.presentation.DownloadButtonUiState

/**
 * In-app browser chrome. [webView] and [fullscreenContainer] are real Views the
 * activity owns — Compose has no web engine, and the page's fullscreen video is
 * attached into [fullscreenContainer] by `WebChromeClient.onShowCustomView`.
 */
private val NavIconTint = Color(0xFF808080)

@Composable
fun WebTabScreen(
    state: WebTabUiState,
    webView: View,
    fullscreenContainer: View,
    bannerAd: View,
    onUrlChange: (String) -> Unit,
    onSubmitUrl: () -> Unit,
    onBack: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onReload: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit,
    onOpenTabs: () -> Unit,
    onDownload: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(AppWhite)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        ) {
            if (!state.isFullscreen) {
                AddressBar(
                    state = state,
                    onUrlChange = onUrlChange,
                    onSubmitUrl = onSubmitUrl,
                    onBack = onBack,
                    onReload = onReload,
                    onOpenTabs = onOpenTabs,
                )

                if (state.showProgress) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        color = Primary,
                        trackColor = AppGray,
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                    )
                }
            }

            RetainedAndroidView(
                view = webView,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            if (!state.isFullscreen) {
                BottomBar(
                    state = state,
                    onNavigateBack = onNavigateBack,
                    onNavigateForward = onNavigateForward,
                    onShare = onShare,
                    onBookmark = onBookmark,
                    onDownload = onDownload
                )

//                RetainedAndroidView(view = bannerAd, modifier = Modifier.fillMaxWidth())
            }
        }

        RetainedAndroidView(
            view = fullscreenContainer,
            modifier = if (state.isFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier.size(0.dp)
            },
        )
    }
}

@Composable
private fun AddressBar(
    state: WebTabUiState,
    onUrlChange: (String) -> Unit,
    onSubmitUrl: () -> Unit,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onOpenTabs: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null,
            tint = NavIconTint,
            modifier = Modifier.size(26.dp).clickable(onClick = onBack),
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(120.dp))
                .background(AppGray)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (state.url.isEmpty()) {
                    Text(
                        text = stringResource(R.string.string_search_for_anything),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SearchFieldHint,
                    )
                }

                BasicTextField(
                    value = state.url,
                    onValueChange = onUrlChange,
                    singleLine = true,
                    cursorBrush = SolidColor(Primary),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppBlack),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onSubmitUrl() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Image(
                painter = painterResource(
                    if (state.isLoadingPage) R.drawable.ic_close else R.drawable.ic_reload,
                ),
                contentDescription = null,
                modifier = Modifier.size(24.dp).clickable(onClick = onReload),
            )
        }

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppGray)
                .clickable(onClick = onOpenTabs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.tabCount.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = Primary
            )
        }

    }
}

@Composable
private fun DownloadAction(
    buttonState: DownloadButtonUiState,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.padding(start = 8.dp).size(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (buttonState) {
            DownloadButtonUiState.Loading -> CircularProgressIndicator(
                color = Primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(40.dp),
            )

            DownloadButtonUiState.Enabled -> Image(
                painter = painterResource(R.drawable.ic_download_enable),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clickable(onClick = onClick),
            )

            DownloadButtonUiState.Disabled -> Image(
                painter = painterResource(R.drawable.ic_download_disable),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun BottomBar(
    state: WebTabUiState,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavAction(
            iconRes = R.drawable.ic_arrow_back,
            enabled = state.canGoBack,
            onClick = onNavigateBack,
            modifier = Modifier.weight(1f),
        )

        NavAction(
            iconRes = R.drawable.ic_arrow_next,
            enabled = state.canGoForward,
            onClick = onNavigateForward,
            modifier = Modifier.weight(1f),
        )

        DownloadAction(
            buttonState = state.downloadButtonState,
            onClick = onDownload,
        )

        NavAction(
            iconRes = R.drawable.ic_share_web,
            enabled = true,
            onClick = onShare,
            modifier = Modifier.weight(1f),
        )

        NavAction(
            iconRes = R.drawable.ic_bookmark_outline_gray,
            enabled = true,
            onClick = onBookmark,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NavAction(
    iconRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = NavIconTint,
            modifier = Modifier
                .size(24.dp)
                .alpha(if (enabled) 1f else 0.35f)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        )
    }
}
