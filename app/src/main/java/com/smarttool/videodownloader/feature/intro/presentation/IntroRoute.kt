package com.smarttool.videodownloader.feature.intro.presentation

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.ui.components.findComponentActivity
import com.smarttool.videodownloader.helper.PreferenceHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Onboarding pager. Finishing it is what marks onboarding as seen. */
@Composable
fun IntroRoute(onFinish: (showPermission: Boolean) -> Unit) {
    val preferenceHelper: PreferenceHelper = koinInject()
    val activity = LocalContext.current.findComponentActivity()

    val pages = remember { buildIntroPages() }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { AdsConstant.requestNativePermission(activity) }

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
        onFinish = {
            // Guards a double tap on the last page from pushing two destinations.
            if (!finished) {
                finished = true
                preferenceHelper.setBoolean(PreferenceHelper.PREF_SHOWED_ONBOARDING, true)
                onFinish(preferenceHelper.showPermission())
            }
        },
    )
}

private const val AUTO_ADVANCE_MILLIS = 10000L
