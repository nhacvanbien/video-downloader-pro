package com.smarttool.videodownloader.ui.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.smarttool.videodownloader.MainActivity
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.intro.presentation.IntroScreen
import com.smarttool.videodownloader.feature.intro.presentation.buildIntroPages
import com.smarttool.videodownloader.helper.PreferenceHelper
import com.smarttool.videodownloader.ui.permission.PermissionActivity
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.SystemUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class IntroActivity : BaseComposeActivity() {
    private val preferenceHelper: PreferenceHelper by inject()

    private var isButtonClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AdsConstant.requestNativePermission(this)

        setContent {
            AppTheme {
                val pages = remember { buildIntroPages() }
                val pagerState = rememberPagerState(pageCount = { pages.size })
                val scope = rememberCoroutineScope()

                // Onboarding auto-advances to the last page if the user idles, matching
                // the countdown the View implementation restarted on every page change.
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
                    onFinish = ::nextScreen,
                )
            }
        }
    }

    private fun nextScreen() {
        if (isButtonClicked) return
        isButtonClicked = true

        preferenceHelper.setBoolean(PreferenceHelper.PREF_SHOWED_ONBOARDING, true)

        if (preferenceHelper.showPermission()) {
            startActivity(PermissionActivity.newIntent(this))
        } else {
            startActivity(MainActivity.newIntent(this))
        }

        finish()
    }

    companion object {
        private const val AUTO_ADVANCE_MILLIS = 10000L

        fun newIntent(context: Context): Intent {
            return Intent(context, IntroActivity::class.java)
        }
    }
}
