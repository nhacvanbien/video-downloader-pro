package com.smarttool.videodownloader.feature.downloads.presentation

import android.view.View
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.AppTopBar
import com.smarttool.videodownloader.core.ui.components.NativeAdContainer
import com.smarttool.videodownloader.core.ui.components.RetainedAndroidView
import com.smarttool.videodownloader.core.ui.components.ScreenGradient
import com.smarttool.videodownloader.core.ui.components.SheetCornerRadius
import com.smarttool.videodownloader.core.ui.theme.AppBlack
import com.smarttool.videodownloader.core.ui.theme.AppGray
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.SearchFieldHint
import com.smarttool.videodownloader.core.ui.theme.TextPrimary
import com.smarttool.videodownloader.core.ui.theme.TextSub
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState

@Composable
fun ProcessingScreen(
    state: ProcessingUiState,
    downloads: List<ProgressInfo>,
    detectionWebView: View,
    onUrlChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onGuideClick: () -> Unit,
    onPauseResume: (ProgressInfo) -> Unit,
    onCancel: (ProgressInfo) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(ScreenGradient)) {
        AppTopBar(
            title = stringResource(R.string.string_processing),
            trailing = {
                Image(
                    painter = painterResource(R.drawable.ic_guide),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clickable(onClick = onGuideClick),
                )
            },
        )

        PasteRow(
            state = state,
            onUrlChange = onUrlChange,
            onPasteClick = onPasteClick,
            onDownloadClick = onDownloadClick,
        )

        // Off-screen WebView that loads the pasted URL so the detection pipeline can
        // scan it. It must stay attached and laid out, so it is sized to 1dp rather
        // than removed from composition.
        RetainedAndroidView(view = detectionWebView, modifier = Modifier.size(1.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius))
                .background(AppWhite),
        ) {
            if (downloads.isEmpty()) {
                EmptyProcessing()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(downloads, key = { it.id }) { info ->
                        DownloadRow(
                            info = info,
                            onPauseResume = { onPauseResume(info) },
                            onCancel = { onCancel(info) },
                        )
                    }
                }
            }
        }

        NativeAdContainer(
            adUnitId = BuildConfig.NATIVE_SMALL_ALL,
            layoutRes = R.layout.layout_native_ad_small_bottom,
            adPlacement = "native_processing",
            canShowAds = AdsConstant.showNativeSmallAll,
        )
    }
}

@Composable
private fun PasteRow(
    state: ProcessingUiState,
    onUrlChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onDownloadClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(120.dp))
                .background(AppWhite)
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (state.url.isEmpty()) {
                    Text(
                        text = stringResource(R.string.string_paste_link_here),
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = stringResource(R.string.string_paste),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = Primary,
                modifier = Modifier.clickable(onClick = onPasteClick).padding(horizontal = 8.dp),
            )
        }

        DownloadButton(
            buttonState = state.downloadButtonState,
            onClick = onDownloadClick,
        )
    }
}

@Composable
private fun DownloadButton(
    buttonState: DownloadButtonUiState,
    onClick: () -> Unit,
) {
    when (buttonState) {
        DownloadButtonUiState.Loading -> Box(
            modifier = Modifier.padding(start = 8.dp).size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = AppWhite,
                strokeWidth = 3.dp,
                modifier = Modifier.size(26.dp),
            )
        }

        DownloadButtonUiState.Enabled -> Image(
            painter = painterResource(R.drawable.ic_download_enable),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp).size(40.dp).clickable(onClick = onClick),
        )

        DownloadButtonUiState.Disabled -> Image(
            painter = painterResource(R.drawable.ic_download_disable_update),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp).size(40.dp),
        )
    }
}

@Composable
private fun DownloadRow(
    info: ProgressInfo,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
) {
    val isPaused = info.downloadStatus == VideoTaskState.PAUSE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = info.videoInfo.title,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Image(
                painter = painterResource(
                    if (isPaused) R.drawable.ic_play else R.drawable.ic_pause,
                ),
                contentDescription = null,
                modifier = Modifier.size(24.dp).clickable(onClick = onPauseResume),
            )

            Image(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(20.dp)
                    .clickable(onClick = onCancel),
            )
        }

        LinearProgressIndicator(
            progress = { info.progress / 100f },
            color = Primary,
            trackColor = AppGray,
            modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 8.dp),
        )

        Text(
            text = info.progressSize,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
            color = TextSub,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun EmptyProcessing() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painter = painterResource(R.drawable.ic_airboat), contentDescription = null)

        Text(
            text = stringResource(R.string.string_download_not_found),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSub,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 40.dp, end = 40.dp),
        )
    }
}
