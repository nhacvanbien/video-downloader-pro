package com.smarttool.videodownloader.feature.main.presentation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.feature.browser.presentation.BrowserHomeRoute
import com.smarttool.videodownloader.feature.browser.presentation.WebTabRoute
import com.smarttool.videodownloader.feature.browser.presentation.WebTabViewHost
import com.smarttool.videodownloader.feature.downloads.presentation.DownloadsRoute
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingWebViewHost
import com.smarttool.videodownloader.feature.settings.presentation.SettingsRoute

/** Window in which a second back press confirms the exit instead of just re-arming the toast. */
private const val EXIT_BACK_PRESS_WINDOW_MS = 2000L

/**
 * The three-tab home. It is the graph's start destination, so back here means "leave the
 * app" — hence the double-back-to-exit gesture: the first press only shows a toast, the
 * second press within [EXIT_BACK_PRESS_WINDOW_MS] fires [onExitRequested].
 */
@Composable
fun MainRoute(
    selectedTab: MainTab,
    processingHost: ProcessingWebViewHost,
    webTabHost: WebTabViewHost,
    onSelectTab: (MainTab) -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenGuide: (from: String) -> Unit,
    onOpenTabs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPrivateArea: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenMedia: (VideoTaskItem) -> Unit,
    onPreviewMedia: (url: String, title: String, headers: String) -> Unit,
    onExitRequested: () -> Unit,
) {
    val context = LocalContext.current
    var lastBackPressTimeMs by remember { mutableLongStateOf(0L) }

    BackHandler {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTimeMs < EXIT_BACK_PRESS_WINDOW_MS) {
            onExitRequested()
        } else {
            lastBackPressTimeMs = now
            Toast.makeText(context, R.string.string_press_back_again_to_exit, Toast.LENGTH_SHORT).show()
        }
    }

    // The Downloads tab advertises how many downloads are still running, so the count is
    // visible from the other two tabs.
    val activeDownloads by processingHost.processingViewModel.downloads.collectAsStateWithLifecycle()

    MainScreen(
        selectedTab = selectedTab,
        onSelectTab = onSelectTab,
        badgeCounts = mapOf(MainTab.Downloads to activeDownloads.size),
    ) { tab ->
        when (tab) {
            MainTab.Browser -> if (webTabHost.started) {
                WebTabRoute(
                    host = webTabHost,
                    onOpenTabs = onOpenTabs,
                    onPreviewMedia = onPreviewMedia,
                    onBack = { webTabHost.release() },
                )
            } else {
                BrowserHomeRoute(
                    onOpenUrl = onOpenUrl,
                    onOpenGuide = { onOpenGuide(GUIDE_FROM_HOME) },
                    onOpenTabs = onOpenTabs,
                    onOpenBookmarks = onOpenBookmarks,
                    onOpenHistory = onOpenHistory,
                    onOpenMedia = onOpenMedia,
                    onSeeAllDownloads = { onSelectTab(MainTab.Downloads) },
                )
            }

            MainTab.Downloads -> DownloadsRoute(
                host = processingHost,
                onOpenGuide = { onOpenGuide(GUIDE_FROM_PROCESS) },
                onPreviewMedia = onPreviewMedia,
                onOpenMedia = onOpenMedia,
                onOpenPrivateArea = onOpenPrivateArea,
            )

            MainTab.Settings -> SettingsRoute(onOpenLanguage = onOpenLanguage)
        }
    }
}

const val GUIDE_FROM_HOME = "home"
const val GUIDE_FROM_PROCESS = "process"
