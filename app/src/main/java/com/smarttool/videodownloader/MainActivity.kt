package com.smarttool.videodownloader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.banner.BannerAdConfig
import com.ads.admob.helper.banner.BannerAdHelper
import com.ads.admob.helper.banner.params.BannerAdParam
import com.ads.admob.listener.BannerAdCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.databinding.LayoutBannerContainerBinding
import com.smarttool.videodownloader.core.UpdateEvent
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.ads.InterAdsManager
import com.smarttool.videodownloader.core.ads.showInterAll
import com.smarttool.videodownloader.core.navigation.AppNavHost
import com.smarttool.videodownloader.core.navigation.AppRoute
import com.smarttool.videodownloader.core.permission.MediaPermissionChecker
import com.smarttool.videodownloader.core.permission.StoragePermissionSheet
import com.smarttool.videodownloader.core.ui.dialogs.DialogExitApp
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.core.withAppLocale
import com.smarttool.videodownloader.data.downloader.youtubedl_downloader.YoutubeDlDownloaderWorker
import com.smarttool.videodownloader.feature.browser.presentation.WebTabViewHost
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingWebViewHost
import com.smarttool.videodownloader.feature.main.presentation.MainContract
import com.smarttool.videodownloader.feature.main.presentation.MainTab
import com.smarttool.videodownloader.feature.main.presentation.MainViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.koin.android.ext.android.inject
import kotlin.system.exitProcess
import org.koin.androidx.viewmodel.ext.android.viewModel as koinViewModel

/**
 * The app's only screen host. Everything except the splash lives in [AppNavHost].
 *
 * The Activity still owns three things the composition cannot: the WebView-backed
 * controllers (they have to outlive their destination's composable), the shared
 * permission sheet (its result launchers belong to the Activity), and the ad SDK
 * surfaces, which are Views.
 */
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by koinViewModel()
    private val permissionChecker: MediaPermissionChecker by inject()

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

    private val bannerBinding by lazy { LayoutBannerContainerBinding.inflate(layoutInflater) }

    private val bannerAdHelper by lazy {
        BannerAdHelper(
            activity = this,
            lifecycleOwner = this,
            config = BannerAdConfig(
                idAds = BuildConfig.BANNER_ALL,
                canShowAds = AdsConstant.showBannerAll,
                canReloadAds = true,
                adPlacement = "banner_home",
            ),
        )
    }

    /** Applied here, not in [onCreate], so resources resolve in the picked language from the start. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
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

        loadAd()

        // The full-screen native ad is a destination now, so the ad manager needs a way
        // to reach the graph instead of starting an Activity of its own.
        InterAdsManager.openNativeFull = { openNativeFull?.invoke() }

        selectedTab = initialTab()
        processingHost.start()

        setContent {
            val navController = rememberNavController()

            SideEffect { openNativeFull = { navController.navigate(AppRoute.NATIVE_FULL) } }

            AppTheme {
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination(),
                    selectedTab = selectedTab,
                    mainBannerAd = bannerBinding.root,
                    processingHost = processingHost,
                    webTabHost = webTabHost,
                    onSelectTab = { selectedTab = it },
                    showInterstitial = { onDone -> showInterAll(onDone) },
                    onExitRequested = ::showDialogExitApp,
                )
            }
        }
    }

    /** Set once the NavHost exists; see [InterAdsManager.openNativeFull]. */
    private var openNativeFull: (() -> Unit)? = null

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

        if (isFinished) {
            val hadError = intent.getBooleanExtra(
                YoutubeDlDownloaderWorker.IS_FINISHED_DOWNLOAD_ACTION_ERROR_KEY,
                false,
            )
            return if (hadError) MainTab.Processing else MainTab.Downloaded
        }

        val hasKey = intent?.hasExtra(
            YoutubeDlDownloaderWorker.IS_FINISHED_DOWNLOAD_ACTION_KEY,
        ) == true

        return if (hasKey) MainTab.Processing else MainTab.Browser
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
        InterAdsManager.openNativeFull = null
        openNativeFull = null
        processingHost.release()
        webTabHost.release()
    }

    private fun showDialogExitApp() {
        EventBus.getDefault().post(UpdateEvent("hide_ads"))

        val dialogExitApp = DialogExitApp(this) {
            mainViewModel.onEvent(MainContract.Event.ConfirmExit)
        }

        dialogExitApp.show()

        dialogExitApp.setOnDismissListener {
            EventBus.getDefault().post(UpdateEvent("show_ads"))
        }
    }

    private fun loadAd() {
        if (AdsConstant.showBannerAll) {
            bannerAdHelper.setBannerContentView(bannerBinding.frAdsBanner)
            bannerAdHelper.requestAds(BannerAdParam.Request)
            bannerAdHelper.registerAdListener(object : BannerAdCallBack {
                override fun onAdImpression() = Unit
                override fun onAdClicked() = Unit
                override fun onAdFailedToLoad(loadAdError: LoadAdError) = Unit
                override fun onAdFailedToShow(adError: AdError) = Unit
                override fun onAdLoaded(data: ContentAd) = Unit
            })
        }

        InterAdsManager.requestInter(this, InterAdsManager.INTER_ALL)
    }

    companion object {
        const val EXTRA_START_DESTINATION = "start_destination"

        fun newIntent(context: Context, startDestination: String? = null): Intent =
            Intent(context, MainActivity::class.java).apply {
                startDestination?.let { putExtra(EXTRA_START_DESTINATION, it) }
            }
    }
}
