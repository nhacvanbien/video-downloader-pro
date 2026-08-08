package com.smarttool.videodownloader.feature.tab.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import org.koin.androidx.compose.koinViewModel

/** The open-tabs list. Every way out of it opens the browser on a URL. */
@Composable
fun TabsRoute(
    onOpenUrl: (String) -> Unit,
) {
    val viewModel: TabsViewModel = koinViewModel()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TabsContract.Effect.TabOpened -> onOpenUrl(effect.tab.url)
                is TabsContract.Effect.TabCreated -> onOpenUrl(effect.tab.url)
            }
        }
    }

    TabsScreen(
        tabs = tabs,
        onOpenTab = { tab ->
            if (tab.url.isNotEmpty()) {
                viewModel.onEvent(TabsContract.Event.OpenTab(tab))
            }
        },
        onDeleteTab = { viewModel.onEvent(TabsContract.Event.DeleteTab(it)) },
        onCloseAll = { viewModel.onEvent(TabsContract.Event.CloseAll) },
        onNewTab = {
            viewModel.onEvent(
                TabsContract.Event.CreateTab(WebTabFactory.createTabModelFromInput(DEFAULT_TAB_URL)),
            )
        },
    )
}

private const val DEFAULT_TAB_URL = "https://www.google.com"
