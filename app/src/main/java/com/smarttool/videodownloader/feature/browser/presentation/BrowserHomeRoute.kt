package com.smarttool.videodownloader.feature.browser.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import org.koin.androidx.compose.koinViewModel

/**
 * The Browser tab: search box plus popular-site tiles. Every entry point here is an
 * interstitial slot, which is why [showInterstitial] wraps each one.
 */
@Composable
fun BrowserHomeRoute(
    showInterstitial: (onDone: () -> Unit) -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenGuide: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val viewModel: BrowserHomeViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BrowserHomeContract.Effect.TabReady -> onOpenUrl(effect.tab.url)
            }
        }
    }

    val openTab: (String) -> Unit = { input ->
        viewModel.onEvent(
            BrowserHomeContract.Event.OpenTab(WebTabFactory.createTabModelFromInput(input)),
        )
    }

    BrowserHomeScreen(
        state = state,
        sites = viewModel.sites,
        onQueryChange = { viewModel.onEvent(BrowserHomeContract.Event.QueryChange(it)) },
        onSubmitQuery = {
            if (state.query.isNotEmpty()) {
                showInterstitial {
                    openTab(state.query)
                    viewModel.onEvent(BrowserHomeContract.Event.ClearQuery)
                }
            }
        },
        onOpenSite = { site -> showInterstitial { openTab(site.url) } },
        onOpenGuide = { showInterstitial(onOpenGuide) },
        onOpenTabs = { showInterstitial(onOpenTabs) },
        onOpenBookmarks = { showInterstitial(onOpenBookmarks) },
        onOpenHistory = { showInterstitial(onOpenHistory) },
    )
}
