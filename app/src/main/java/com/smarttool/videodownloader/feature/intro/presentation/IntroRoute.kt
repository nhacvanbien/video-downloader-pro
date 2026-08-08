package com.smarttool.videodownloader.feature.intro.presentation

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.ui.components.findComponentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** Onboarding pager. Finishing it is what marks onboarding as seen. */
@Composable
fun IntroRoute(onFinish: (showPermission: Boolean) -> Unit) {
    val viewModel: IntroViewModel = koinViewModel()
    val activity = LocalContext.current.findComponentActivity()

    val pages = remember { buildIntroPages() }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { AdsConstant.requestNativePermission(activity) }

    LaunchedEffect(Unit) {
        viewModel.finished.collect { showPermission -> onFinish(showPermission) }
    }

    // Onboarding auto-advances to the last page if the user idles, matching the
    // countdown the View implementation restarted on every page change.
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == pages.lastIndex) return@LaunchedEffect
        delay(AUTO_ADVANCE_MILLIS)
        pagerState.animateScrollToPage(pages.lastIndex)
    }

    IntroScreen(
        pages = pages,
        pagerState = pagerState,
        onNext = {
            scope.launch {
                pagerState.animateScrollToPage(
                    (pagerState.currentPage + 1).coerceAtMost(pages.lastIndex),
                )
            }
        },
        onFinish = viewModel::finish,
    )
}

private const val AUTO_ADVANCE_MILLIS = 10000L
