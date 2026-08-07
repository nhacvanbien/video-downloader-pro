package com.smarttool.videodownloader.feature.tab.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import org.koin.androidx.compose.koinViewModel

/** The open-tabs list. Every way out of it opens the browser on a URL. */
@Composable
fun TabsRoute(
    showInterstitial: (onDone: () -> Unit) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val viewModel: TabsViewModel = koinViewModel()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()

    TabsScreen(
        tabs = tabs,
        onOpenTab = { tab ->
            showInterstitial {
                if (tab.url.isNotEmpty()) {
                    viewModel.onOpenTab(tab) { onOpenUrl(tab.url) }
                }
            }
        },
        onDeleteTab = viewModel::onDeleteTab,
        onCloseAll = viewModel::onCloseAll,
        onNewTab = {
            viewModel.onCreateTab(WebTabFactory.createTabModelFromInput(DEFAULT_TAB_URL)) {
                onOpenUrl(DEFAULT_TAB_URL)
            }
        },
    )
}

private const val DEFAULT_TAB_URL = "https://www.google.com"
