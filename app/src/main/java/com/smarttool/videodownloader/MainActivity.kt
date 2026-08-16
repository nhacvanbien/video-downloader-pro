package com.smarttool.videodownloader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.core.navigation.AppNavHost
import com.smarttool.videodownloader.core.navigation.AppRoute
import com.smarttool.videodownloader.core.permission.MediaPermissionChecker
import com.smarttool.videodownloader.core.permission.StoragePermissionSheet
import com.smarttool.videodownloader.core.ui.theme.AppLocaleProvider
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.core.withAppLocale
import com.smarttool.videodownloader.data.downloader.youtubedl_downloader.YoutubeDlDownloaderWorker
import com.smarttool.videodownloader.feature.browser.presentation.WebTabViewHost
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingWebViewHost
import com.smarttool.videodownloader.feature.main.presentation.MainContract
import com.smarttool.videodownloader.feature.main.presentation.MainTab
import com.smarttool.videodownloader.feature.main.presentation.MainViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.system.exitProcess
import org.koin.androidx.viewmodel.ext.android.viewModel as koinViewModel

/**
 * The app's only screen host. Everything except the splash lives in [AppNavHost].
 *
 * The Activity still owns two things the composition cannot: the WebView-backed
 * controllers (they have to outlive their destination's composable) and the shared
 * permission sheet (its result launchers belong to the Activity).
 */
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by koinViewModel()
    private val permissionChecker: MediaPermissionChecker by inject()
    private val preferences: AppPreferencesDataSource by inject()

    private var selectedTab by mutableStateOf(MainTab.Browser)

    private val permissionSheet by lazy {
        StoragePermissionSheet(this, permissionChecker)
    }

    private val processingHost by lazy {
        ProcessingWebViewHost(this, permissionSheet, permissionChecker)
    }

    private val webTabHost by lazy {
        WebTabViewHost(this, permissionSheet, permissionChecker)
    }

    /** Applied here, not in [onCreate], so legacy View-based screens resolve resources
     *  in the picked language from the start. Compose picks up live changes via
     *  [AppLocaleProvider] instead, without recreating the Activity. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale(preferences))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            mainViewModel.effect.collect { effect ->
                when (effect) {
                    is MainContract.Effect.ExitRecorded -> {
                        finishAffinity()
                        exitProcess(1)
                    }
                }
            }
        }

        selectedTab = initialTab()
        processingHost.start()

        setContent {
            val navController = rememberNavController()

            AppLocaleProvider {
                AppTheme {
                    AppNavHost(
                        navController = navController,
                        startDestination = startDestination(),
                        selectedTab = selectedTab,
                        processingHost = processingHost,
                        webTabHost = webTabHost,
                        onSelectTab = { selectedTab = it },
                        onExitRequested = { mainViewModel.onEvent(MainContract.Event.ConfirmExit) },
                    )
                }
            }
        }
    }

    /**
     * The splash decides where the graph starts, so onboarding steps do not have to be
     * Activities just to be reachable before the home screen exists.
     */
    private fun startDestination(): String =
        intent?.getStringExtra(EXTRA_START_DESTINATION) ?: AppRoute.MAIN

    /** The notification that launched us decides which tab opens first. */
    private fun initialTab(): MainTab {
        val isFinished = intent?.getBooleanExtra(
            YoutubeDlDownloaderWorker.IS_FINISHED_DOWNLOAD_ACTION_KEY,
            false,
        ) == true

        if (isFinished) return MainTab.Downloads

        val hasKey = intent?.hasExtra(
            YoutubeDlDownloaderWorker.IS_FINISHED_DOWNLOAD_ACTION_KEY,
        ) == true

        return if (hasKey) MainTab.Downloads else MainTab.Browser
    }

    /** The browser registers its WebView for a context menu; only the Activity is asked. */
    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View,
        menuInfo: ContextMenuInfo?,
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        webTabHost.onCreateContextMenu(v)
    }

    override fun onPause() {
        super.onPause()
        processingHost.onActivityPause()
        webTabHost.onActivityPause()
    }

    override fun onResume() {
        super.onResume()
        processingHost.onActivityResume()
        webTabHost.onActivityResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        processingHost.release()
        webTabHost.release()
    }

    companion object {
        const val EXTRA_START_DESTINATION = "start_destination"

        fun newIntent(context: Context, startDestination: String? = null): Intent =
            Intent(context, MainActivity::class.java).apply {
                startDestination?.let { putExtra(EXTRA_START_DESTINATION, it) }
            }
    }
}
