package com.smarttool.videodownloader.feature.browser.presentation

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import org.koin.androidx.compose.koinViewModel

/** The Browser tab: paste-link card, shortcuts, popular-site tiles and recent downloads. */
@Composable
fun BrowserHomeRoute(
    onOpenUrl: (String) -> Unit,
    onOpenGuide: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMedia: (VideoTaskItem) -> Unit,
    onSeeAllDownloads: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: BrowserHomeViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentDownloads by viewModel.recentDownloads.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BrowserHomeContract.Effect.TabReady -> onOpenUrl(effect.tab.url)
            }
        }
    }

    val openTab: (String) -> Unit = { input ->
        viewModel.onEvent(
            BrowserHomeContract.Event.OpenTab(viewModel.tabModelFromInput(input)),
        )
    }

    BrowserHomeScreen(
        state = state,
        sites = viewModel.sites,
        recentDownloads = recentDownloads,
        onQueryChange = { viewModel.onEvent(BrowserHomeContract.Event.QueryChange(it)) },
        onSubmitQuery = {
            if (state.query.isNotEmpty()) {
                openTab(state.query)
                viewModel.onEvent(BrowserHomeContract.Event.ClearQuery)
            }
        },
        onPasteFromClipboard = {
            val pasted = readClipboardText(context)
            if (pasted != null) {
                viewModel.onEvent(BrowserHomeContract.Event.QueryChange(pasted))
            }
        },
        onOpenSite = { site -> openTab(site.url) },
        onOpenGuide = onOpenGuide,
        onOpenTabs = onOpenTabs,
        onOpenBookmarks = onOpenBookmarks,
        onOpenHistory = onOpenHistory,
        onOpenMedia = onOpenMedia,
        onSeeAllDownloads = onSeeAllDownloads,
    )
}

/** Null with a toast already shown when the clipboard holds nothing usable. */
private fun readClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val hasText = clipboard.hasPrimaryClip() &&
        clipboard.primaryClipDescription
            ?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true

    if (!hasText) {
        Toast.makeText(
            context,
            context.getString(R.string.string_no_text_in_clipboard),
            Toast.LENGTH_SHORT,
        ).show()
        return null
    }

    val copied = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()

    if (copied.isEmpty()) {
        Toast.makeText(
            context,
            context.getString(R.string.string_clipboard_is_empty),
            Toast.LENGTH_SHORT,
        ).show()
        return null
    }

    return copied
}
