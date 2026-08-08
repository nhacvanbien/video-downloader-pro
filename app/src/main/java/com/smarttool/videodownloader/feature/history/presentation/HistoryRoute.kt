package com.smarttool.videodownloader.feature.history.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.ui.components.NativeAdContainer
import com.smarttool.videodownloader.core.ui.dialogs.DialogAddBookmark
import com.smarttool.videodownloader.core.ui.dialogs.DialogConfirmDelete
import com.smarttool.videodownloader.data.repository.TabModelRepository
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/** History and bookmarks; [mode] picks which of the two the same screen renders. */
@Composable
fun HistoryRoute(
    mode: HistoryMode,
    showInterstitial: (onDone: () -> Unit) -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: HistoryViewModel = koinViewModel { parametersOf(mode) }
    val tabModelRepository: TabModelRepository = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val leave = { showInterstitial(onBack) }

    BackHandler { leave() }

    HistoryScreen(
        state = state,
        onBack = leave,
        onSearchQueryChange = { viewModel.onEvent(HistoryContract.Event.SearchQueryChange(it)) },
        onSearchActiveChange = { viewModel.onEvent(HistoryContract.Event.SearchActiveChange(it)) },
        onEntryClick = { entry ->
            // The tab has to exist before the browser opens, or it reads back null and
            // creates a duplicate for the same page.
            scope.launch(Dispatchers.IO) {
                tabModelRepository.insertTabModel(
                    WebTabFactory.createTabModelFromInput(entry.url),
                )
            }
            onOpenUrl(entry.url)
        },
        onDeleteEntry = { entry ->
            DialogConfirmDelete(context) {
                viewModel.onEvent(HistoryContract.Event.DeleteEntry(entry))
            }.show()
        },
        onClearHistory = { viewModel.onEvent(HistoryContract.Event.ClearHistory) },
        onAddBookmark = {
            DialogAddBookmark(context) { name, url ->
                viewModel.onEvent(HistoryContract.Event.AddBookmark(name, url))
            }.show()
        },
        adSlot = {
            NativeAdContainer(
                adUnitId = BuildConfig.NATIVE_SMALL_ALL,
                layoutRes = R.layout.layout_native_ad_small_bottom,
                adPlacement = "native_history",
                canShowAds = AdsConstant.showNativeSmallAll,
            )
        },
    )
}
