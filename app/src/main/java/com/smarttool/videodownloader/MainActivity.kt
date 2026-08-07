package com.smarttool.videodownloader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.UpdateEvent
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.ads.InterAdsManager
import com.smarttool.videodownloader.core.ads.showInterAll
import com.smarttool.videodownloader.core.navigation.AppNavHost
import com.smarttool.videodownloader.core.navigation.AppRoute
import com.smarttool.videodownloader.core.permission.MediaPermissionChecker
import com.smarttool.videodownloader.core.permission.StoragePermissionSheet
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.data.downloader.youtubedl_downloader.YoutubeDlDownloaderWorker
import com.smarttool.videodownloader.dialog.DialogExitApp
import com.smarttool.videodownloader.feature.browser.presentation.WebTabController
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingController
import com.smarttool.videodownloader.feature.main.presentation.MainTab
import com.smarttool.videodownloader.feature.tab.presentation.TabViewModel
import com.smarttool.videodownloader.helper.PreferenceHelper
import org.greenrobot.eventbus.EventBus
import org.koin.android.ext.android.inject
import kotlin.system.exitProcess

/**
 * The app's only screen host. Everything except the splash lives in [AppNavHost].
 *
 * The Activity still owns three things the composition cannot: the WebView-backed
 * controllers (they have to outlive their destination's composable), the shared
 * permission sheet (its result launchers belong to the Activity), and the ad SDK
 * surfaces, which are Views.
 */
class MainActivity : BaseComposeActivity() {

    private val preferenceHelper: PreferenceHelper by inject()
    private val permissionChecker: MediaPermissionChecker by inject()

    private var selectedTab by mutableStateOf(MainTab.Browser)

    /** Global tab store, shared with the download workers. */
    private val tabViewModels: TabViewModel by lazy {
        (application as VideoDownloaderApplication).globalViewModel
    }

    private val permissionSheet by lazy {
        StoragePermissionSheet(this, permissionChecker)
    }

    private val processingController by lazy {
        ProcessingController(this, permissionSheet, permissionChecker)
    }

    private val webTabController by lazy {
        WebTabController(this, permissionSheet, permissionChecker)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadAd()

        // The full-screen native ad is a destination now, so the ad manager needs a way
        // to reach the graph instead of starting an Activity of its own.
        InterAdsManager.openNativeFull = { openNativeFull?.invoke() }

        selectedTab = initialTab()
        processingController.start()

        setContent {
            val navController = rememberNavController()

            SideEffect { openNativeFull = { navController.navigate(AppRoute.NATIVE_FULL) } }

            AppTheme {
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination(),
                    selectedTab = selectedTab,
                    mainBannerAd = bannerBinding.root,
                    processingController = processingController,
                    webTabController = webTabController,
                    onSelectTab = { selectedTab = it },
                    showInterstitial = { onDone -> showInterAll(onDone) },
                    onExitRequested = ::showDialogExitApp,
                )
            }
        }

        handleFinishedDownloadIntent()
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

    private fun handleFinishedDownloadIntent() {
        if (intent?.hasExtra(YoutubeDlDownloaderWorker.DOWNLOAD_FILENAME_KEY) != true) return

        val downloadFileName =
            intent.getStringExtra(YoutubeDlDownloaderWorker.DOWNLOAD_FILENAME_KEY).orEmpty()

        // The library screen needs a beat to compose before it can react to the event.
        Handler(Looper.getMainLooper()).postDelayed({
            tabViewModels.openDownloadedVideoEvent.value = downloadFileName
        }, OPEN_DOWNLOADED_DELAY_MILLIS)
    }

    /** The browser registers its WebView for a context menu; only the Activity is asked. */
    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View,
        menuInfo: ContextMenuInfo?,
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        webTabController.onCreateContextMenu(v)
    }

    override fun onPause() {
        super.onPause()
        processingController.onActivityPause()
        webTabController.onActivityPause()
    }

    override fun onResume() {
        super.onResume()
        processingController.onActivityResume()
        webTabController.onActivityResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        InterAdsManager.openNativeFull = null
        openNativeFull = null
        processingController.release()
        webTabController.release()
    }

    private fun showDialogExitApp() {
        EventBus.getDefault().post(UpdateEvent("hide_ads"))

        val dialogExitApp = DialogExitApp(this) {
            preferenceHelper.increaseCountExitApp()

            finishAffinity()
            exitProcess(1)
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
        private const val OPEN_DOWNLOADED_DELAY_MILLIS = 1000L

        const val EXTRA_START_DESTINATION = "start_destination"

        fun newIntent(context: Context, startDestination: String? = null): Intent =
            Intent(context, MainActivity::class.java).apply {
                startDestination?.let { putExtra(EXTRA_START_DESTINATION, it) }
            }
    }
}
