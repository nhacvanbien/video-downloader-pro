package com.smarttool.videodownloader

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.banner.BannerAdConfig
import com.ads.admob.helper.banner.BannerAdHelper
import com.ads.admob.helper.banner.params.BannerAdParam
import com.ads.admob.listener.BannerAdCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.play.core.review.ReviewManagerFactory
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.base.BaseActivity
import com.smarttool.videodownloader.data.remote.service.AdBlockHostsRemoteDataSource
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.smarttool.videodownloader.android.databinding.LayoutBannerContainerBinding
import com.smarttool.videodownloader.android.databinding.LayoutMainContentBinding
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.main.presentation.MainScreen
import com.smarttool.videodownloader.feature.main.presentation.MainTab
import com.smarttool.videodownloader.dialog.DialogExitApp
import com.smarttool.videodownloader.dialog.RatingDialog
import com.smarttool.videodownloader.helper.PreferenceHelper
import com.smarttool.videodownloader.ui.downloaded.DownloadedFragment
import com.smarttool.videodownloader.ui.processing.ProcessingFragment
import com.smarttool.videodownloader.ui.settings.SettingsFragment
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.AppConstant
import com.smarttool.videodownloader.core.ads.InterAdsManager
import com.smarttool.videodownloader.core.UpdateEvent
import com.smarttool.videodownloader.data.downloader.youtubedl_downloader.YoutubeDlDownloaderWorker
import com.vimalcvs.materialrating.DialogManager
import org.greenrobot.eventbus.EventBus
import kotlin.system.exitProcess
import android.os.Bundle
import com.smarttool.videodownloader.feature.tab.presentation.TabViewModel
import androidx.core.view.doOnAttach
import org.koin.android.ext.android.inject
import com.smarttool.videodownloader.ui.browser.BrowserFragment

class MainActivity : BaseComposeActivity() {
    private val preferenceHelper: PreferenceHelper by inject()

    // Color
    private var colorSelected = 0
    private var colorNormal = 0

    @DrawableRes
    private var drawableBrowserNormal: Int = 0

    @DrawableRes
    private var drawableBrowserSelected: Int = 0

    @DrawableRes
    private var drawableProcessingNormal: Int = 0

    @DrawableRes
    private var drawableProcessingSelected: Int = 0

    @DrawableRes
    private var drawableDownloadedNormal: Int = 0

    @DrawableRes
    private var drawableDownloadedSelected: Int = 0

    @DrawableRes
    private var drawableSettingsNormal: Int = 0

    @DrawableRes
    private var drawableSettingsSelected: Int = 0

    private var posSelectedNavigation = 0
    private val adBlockHostsRemoteDataSource: AdBlockHostsRemoteDataSource by inject()

    private var selectedTab by mutableStateOf(MainTab.Browser)

    /** Global tab store, previously inherited from BaseActivity. */
    private val tabViewModels: TabViewModel by lazy {
        (application as VideoDownloaderApplication).globalViewModel
    }

    private val contentBinding by lazy { LayoutMainContentBinding.inflate(layoutInflater) }

    private val bannerBinding by lazy { LayoutBannerContainerBinding.inflate(layoutInflater) }

    private val bannerAdHelper by lazy { initBannerAd() }
    private fun initBannerAd(): BannerAdHelper {
        val config = BannerAdConfig(
            idAds = BuildConfig.BANNER_ALL,
            canShowAds = AdsConstant.showBannerAll,
            canReloadAds = true,
            adPlacement = "banner_home",
        )
        return BannerAdHelper(activity = this, lifecycleOwner = this, config = config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadAd()
        DialogManager.showRatingAfterAppOpened(this, AppConstant.FEEDBACK_EMAIL)

        setContent {
            AppTheme {
                MainScreen(
                    selectedTab = selectedTab,
                    tabContent = contentBinding.root,
                    bannerAd = bannerBinding.root,
                    onSelectTab = ::selectTab,
                )
            }
        }

        // The container lives inside the composition, so the first transaction has to
        // wait until Compose has actually attached it to the window.
        contentBinding.frameLayout.doOnAttach {
            selectTab(initialTab(), force = true)
            handleFinishedDownloadIntent()
        }
    }

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

        // The tab fragment needs a beat to attach before it can react to the event.
        Handler(Looper.getMainLooper()).postDelayed({
            tabViewModels.openDownloadedVideoEvent.value = downloadFileName
        }, OPEN_DOWNLOADED_DELAY_MILLIS)
    }

    private fun selectTab(tab: MainTab, force: Boolean = false) {
        if (!force && tab == selectedTab) return

        selectedTab = tab

        val fragment = when (tab) {
            MainTab.Browser -> BrowserFragment()
            MainTab.Processing -> ProcessingFragment()
            MainTab.Downloaded -> DownloadedFragment()
            MainTab.Settings -> SettingsFragment()
        }

        replaceFragment(fragment, tab.name)
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(contentBinding.frameLayout.id, fragment, tag)
        fragmentTransaction.commit()
    }


    override fun onBackPressed() {
        val countOpenApp = preferenceHelper.getCountExitApp()

        showDialogExitApp()
    }

    private fun showDialogExitApp() {
        EventBus.getDefault().post(UpdateEvent("hide_ads"))
        val dialogExitApp = DialogExitApp(this@MainActivity) {
            preferenceHelper.increaseCountExitApp()

            finishAffinity()
            exitProcess(1)
        }

        dialogExitApp.show()

        dialogExitApp.setOnDismissListener {
            EventBus.getDefault().post(UpdateEvent("show_ads"))
        }
    }

    private fun showDialogRate(isExit: Boolean) {
        EventBus.getDefault().post(UpdateEvent("hide_ads"))
        val ratingDialog = RatingDialog(this)
        ratingDialog.init(this, object : RatingDialog.OnPress {
            override fun sendThank() {
                preferenceHelper.forceRated()
                ratingDialog.dismiss()

                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.string_thank_for_rate),
                    Toast.LENGTH_SHORT
                ).show()

                if (isExit) {
                    finishAffinity()

                    exitProcess(1)
                }

            }

            override fun rating() {
                val manager = ReviewManagerFactory.create(this@MainActivity)
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(this@MainActivity, reviewInfo)
                        flow.addOnSuccessListener {
                            preferenceHelper.forceRated()
                            ratingDialog.dismiss()

                            if (isExit) {
                                finishAffinity()
                                exitProcess(1)
                            }

                        }
                    } else {
                        preferenceHelper.forceRated()
                        ratingDialog.dismiss()

                        if (isExit) {
                            finishAffinity()
                            exitProcess(1)
                        }
                    }
                }
            }

            override fun later() {
                ratingDialog.dismiss()

                if (isExit) {

                    preferenceHelper.increaseCountExitApp()

                    finishAffinity()
                    exitProcess(1)
                } else {
                    preferenceHelper.increaseCountBackHome()
                }

            }

        })

        ratingDialog.show()

        ratingDialog.setOnDismissListener {
            EventBus.getDefault().post(UpdateEvent("show_ads"))
        }
    }

    private fun loadAd() {
        if (AdsConstant.showBannerAll) {
            bannerAdHelper.setBannerContentView(bannerBinding.frAdsBanner)
            bannerAdHelper.requestAds(BannerAdParam.Request)
            bannerAdHelper.registerAdListener(object : BannerAdCallBack {
                override fun onAdImpression() {
                }

                override fun onAdClicked() {
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                }

                override fun onAdFailedToShow(adError: AdError) {
                }

                override fun onAdLoaded(data: ContentAd) {
                }
            })
        }
        InterAdsManager.requestInter(this, InterAdsManager.INTER_ALL)

    }

    companion object {
        private const val OPEN_DOWNLOADED_DELAY_MILLIS = 1000L

        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }
}