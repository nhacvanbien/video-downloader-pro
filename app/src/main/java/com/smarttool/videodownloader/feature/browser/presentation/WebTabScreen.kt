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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.RetainedAndroidView
import com.smarttool.videodownloader.core.ui.theme.AppBlack
import com.smarttool.videodownloader.core.ui.theme.AppGray
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.PriSoft
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.SearchFieldHint
import com.smarttool.videodownloader.feature.downloads.presentation.DownloadButtonUiState
import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry

/**
 * In-app browser chrome. [webView] and [fullscreenContainer] are real Views the
 * activity owns — Compose has no web engine, and the page's fullscreen video is
 * attached into [fullscreenContainer] by `WebChromeClient.onShowCustomView`.
 */
private val NavIconTint = Muted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebTabScreen(
    state: WebTabUiState,
    webView: View,
    fullscreenContainer: View,
    onUrlChange: (String) -> Unit,
    onUrlFocusChange: (Boolean) -> Unit,
    onSubmitUrl: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onBack: () -> Unit,
    onReload: () -> Unit,
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
                    onUrlFocusChange = onUrlFocusChange,
                    onSubmitUrl = onSubmitUrl,
                    onBack = onBack,
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

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                var isRefreshing by remember { mutableStateOf(false) }

                // isLoadingPage also flips true on normal navigation (link taps, submits),
                // not just pull gestures — isRefreshing is a separate, gesture-only flag so
                // the pull indicator doesn't pop up on its own; it just rides along until
                // the in-flight load (whatever triggered it) finishes.
                LaunchedEffect(state.isLoadingPage) {
                    if (!state.isLoadingPage) isRefreshing = false
                }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        onReload()
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    RetainedAndroidView(view = webView, modifier = Modifier.fillMaxSize())
                }

                // Chrome-style: focusing the omnibox temporarily replaces the page with
                // matching history, not a small dropdown squeezed above it.
                if (state.isUrlFocused && !state.isFullscreen) {
                    SuggestionsOverlay(
                        query = state.url,
                        suggestions = state.suggestions,
                        onSuggestionClick = onSuggestionClick,
                    )
                }

                if (!state.isFullscreen) {
                    DownloadFab(
                        buttonState = state.downloadButtonState,
                        onClick = onDownload,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    )
                }
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
    onUrlFocusChange: (Boolean) -> Unit,
    onSubmitUrl: () -> Unit,
    onBack: () -> Unit,
    onOpenTabs: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // The field's own focus is separate from `state.isUrlFocused` (the ViewModel drops
    // it once a URL is submitted/a suggestion is picked) — without this the Compose
    // focus system never hears about that and the keyboard/cursor stay stuck.
    LaunchedEffect(state.isUrlFocused) {
        if (!state.isUrlFocused) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

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
                .padding(horizontal = 12.dp, vertical = 12.dp),
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
                    keyboardActions = KeyboardActions(
                        onGo = {
                            onSubmitUrl()
                            keyboardController?.hide()
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { onUrlFocusChange(it.isFocused) },
                )
            }
            Spacer(modifier = Modifier.size(8.dp))

            if (state.isUrlFocused && state.url.isNotEmpty()) {
                Image(
                    painter = painterResource(R.drawable.ic_close_circle_grey),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).clickable { onUrlChange("") },
                )
            }
        }

        Text(
            text = state.tabCount.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = Primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(PriSoft)
                .clickable(onClick = onOpenTabs)
                .padding(top = 4.dp),
        )
    }
}

@Composable
private fun SuggestionsOverlay(
    query: String,
    suggestions: List<HistoryEntry>,
    onSuggestionClick: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(AppWhite)) {
        if (query.isNotBlank()) {
            item(key = "typed-query") {
                SuggestionRow(
                    iconRes = R.drawable.ic_search,
                    title = query,
                    subtitle = null,
                    onClick = { onSuggestionClick(query) },
                )
            }
        }

        items(suggestions, key = { it.id }) { entry ->
            SuggestionRow(
                iconRes = R.drawable.ic_history,
                title = entry.title.ifBlank { entry.url },
                subtitle = entry.url,
                onClick = { onSuggestionClick(entry.url) },
            )
        }
    }
}

@Composable
private fun SuggestionRow(
    iconRes: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )

        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = AppBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SearchFieldHint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DownloadFab(
    buttonState: DownloadButtonUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (buttonState == DownloadButtonUiState.Disabled) Muted else Primary)
            .then(
                // Disabled (nothing detected) stays clickable so the user can retry
                // detection; only Loading (a probe is already in flight) blocks taps.
                if (buttonState != DownloadButtonUiState.Loading) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (buttonState == DownloadButtonUiState.Loading) {
            CircularProgressIndicator(
                color = AppWhite,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            // Disabled is also the retryable state (see the click handling above), so it
            // gets a distinct retry icon rather than the plain download arrow.
            val iconRes = if (buttonState == DownloadButtonUiState.Disabled) {
                R.drawable.ic_reload
            } else {
                R.drawable.ic_download_arrow
            }
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(AppWhite),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
