package com.smarttool.videodownloader.feature.splash.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.cmp.interfaces.OnConsentResponse
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.banner.BannerAdConfig
import com.ads.admob.helper.banner.BannerAdHelper
import com.ads.admob.helper.banner.params.BannerAdParam
import com.ads.admob.helper.interstitial.InterstitialAdSplashConfig
import com.ads.admob.helper.interstitial.InterstitialAdSplashHelper
import com.ads.admob.helper.interstitial.params.InterstitialAdParam
import com.ads.admob.listener.BannerAdCallBack
import com.ads.admob.listener.InterstitialAdCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.smarttool.videodownloader.MainActivity
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.android.databinding.LayoutBannerContainerBinding
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.ads.InAppUpdate
import com.smarttool.videodownloader.core.ads.InstallUpdatedListener
import com.smarttool.videodownloader.core.ads.InterAdsManager
import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.core.navigation.AppRoute
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.core.ui.widget.CustomSeekbarSplash
import com.smarttool.videodownloader.core.ui.widget.ProgressCallback
import com.smarttool.videodownloader.core.withAppLocale
import com.smarttool.videodownloader.feature.library.domain.usecase.PruneMissingFilesUseCase
import com.smarttool.videodownloader.feature.onboarding.domain.model.AppEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import org.koin.androidx.viewmodel.ext.android.viewModel as koinViewModel

class SplashActivity : AppCompatActivity() {
    private val splashViewModel: SplashViewModel by koinViewModel()

    private val preferences: AppPreferencesDataSource by inject()
    private val pruneMissingFiles: PruneMissingFilesUseCase by inject()
    private lateinit var inAppUpdate: InAppUpdate

    private val interRequestCompleteFlow = MutableStateFlow(false)
    private val bannerRequestCompleteFlow = MutableStateFlow(false)

    private var loadingText by mutableStateOf("")
    private var showBanner by mutableStateOf(false)

    private val progressView by lazy {
        CustomSeekbarSplash(this).apply {
            onProgress = object : ProgressCallback {
                override fun onProgress(progress: Int) {
                    loadingText = getString(R.string.string_loading, progress.toString())
                }
            }
        }
    }

    private val bannerBinding by lazy {
        LayoutBannerContainerBinding.inflate(layoutInflater)
    }

    private val bannerContainer: FrameLayout get() = bannerBinding.frAdsBanner

    /** Valid once [SplashViewModel.awaitLoaded] has returned; defaults to "already seen". */
    private val isStartLanguageShowed get() = splashViewModel.uiState.value.startLanguageShown

    private val bannerAdHelper by lazy { initBannerAd() }
    private fun initBannerAd(): BannerAdHelper {
        val config = BannerAdConfig(
            idAds = BuildConfig.BANNER_SPLASH,
            canShowAds = AdsConstant.showBannerSplash,
            canReloadAds = false,
            adPlacement = "banner_splash",
        )

        return BannerAdHelper(activity = this, lifecycleOwner = this, config = config).apply {
            registerAdListener(object : BannerAdCallBack {
                override fun onAdClicked() {
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    bannerRequestCompleteFlow.value = true
                }

                override fun onAdFailedToShow(adError: AdError) {
                    bannerRequestCompleteFlow.value = true
                }

                override fun onAdImpression() {
                }

                override fun onAdLoaded(data: ContentAd) {
                    bannerRequestCompleteFlow.value = true
                }
            })
        }
    }

    private val interAdSplashHelper by lazy { initInterAdSplash() }
    private fun initInterAdSplash(): InterstitialAdSplashHelper {
        val config = InterstitialAdSplashConfig(
            idAds = BuildConfig.INTER_SPLASH,
            idAdsPriority = if (AdsConstant.useInterSplashHigh) BuildConfig.INTER_SPLASH_HIGH else null,
            canShowAds = AdsConstant.showInterSplash,
            canReloadAds = false,
            timeDelay = 5000L,
            timeOut = 30 * 1000L,
            showReady = true,
            adPlacement = "inter_splash",
        )
        return InterstitialAdSplashHelper(
            activity = this,
            lifecycleOwner = this,
            config = config,
            showAfterLoaded = false
        ).apply {
            registerAdListener(interAdCallBack)
        }
    }

    private val interAdCallBack = object : InterstitialAdCallback {
        override fun onNextAction() {
        }

        override fun onAdClose() {
            startNextAct()
        }

        override fun onInterstitialShow() {
        }

        override fun onAdLoaded(data: ContentAd) {
            interRequestCompleteFlow.value = true
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            interRequestCompleteFlow.value = true
        }

        override fun onAdClicked() {
        }

        override fun onAdImpression() {
        }

        override fun onAdFailedToShow(adError: AdError) {
            interRequestCompleteFlow.value = true
        }
    }

    private val consentManager by lazy { ConsentManager.getInstance(this) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale(preferences))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                SplashScreen(
                    loadingText = loadingText,
                    showBanner = showBanner,
                    progressView = progressView,
                    bannerView = bannerBinding.root,
                )
            }
        }

        initFlow()
    }

    private fun initFlow() {
        lifecycleScope.launch {
            pruneMissingFiles()
        }

        consentManager.initReleaseConsent(
            onConsentResponse = object : OnConsentResponse {
                override fun onResponse(errorMessage: String?) {
                    getRemoteConfig {
                        checkRemoteConfigResult()
                    }
                }

                override fun onPolicyRequired(isRequired: Boolean) {
                }
            },
        )

        val remoteConfig = FirebaseRemoteConfig.getInstance()

        inAppUpdate = InAppUpdate(this, remoteConfig.getBoolean("force_update"), object :
            InstallUpdatedListener {
            override fun onUpdateNextAction() {
            }

            override fun onUpdateCancel() {
                finish()
            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (this::inAppUpdate.isInitialized) {
            inAppUpdate.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Onboarding is no longer a chain of Activities: the splash picks where the graph
     * starts and hands that to [MainActivity], which owns every screen from here on.
     */
    private fun startNextAct() {
        lifecycleScope.launch {
            val startDestination = when (splashViewModel.awaitLoaded().entryPoint) {
                AppEntryPoint.Language -> AppRoute.LANGUAGE
                AppEntryPoint.Intro -> AppRoute.INTRO
                AppEntryPoint.Home -> AppRoute.MAIN
            }

            startActivity(
                MainActivity.newIntent(this@SplashActivity, startDestination)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
            finish()
        }
    }

    private fun getRemoteConfig(onComplete: (Boolean) -> Unit) {

        val index = BuildConfig.Minimum_Fetch
        val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(index)
            .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        mFirebaseRemoteConfig.fetchAndActivate()


        mFirebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    AdsConstant.showInterSplash =
                        mFirebaseRemoteConfig.getBoolean("show_inter_splash")
                    AdsConstant.showBannerSplash =
                        mFirebaseRemoteConfig.getBoolean("show_banner_splash")
                    AdsConstant.showNativeLanguage1_1 =
                        mFirebaseRemoteConfig.getBoolean("show_native_language_1_1")
                    AdsConstant.showNativeLanguage1_2 =
                        mFirebaseRemoteConfig.getBoolean("show_native_language_1_2")
                    AdsConstant.showNativeOnboardFullscreen1_1 =
                        mFirebaseRemoteConfig.getBoolean("show_native_onboard_fullscreen_1_1")
                    AdsConstant.showNativeOnboardFullscreen1_2 =
                        mFirebaseRemoteConfig.getBoolean("show_native_onboard_fullscreen_1_2")
                    AdsConstant.showNativePermission =
                        mFirebaseRemoteConfig.getBoolean("show_native_permission")
                    AdsConstant.showNativeHome =
                        mFirebaseRemoteConfig.getBoolean("show_native_home")
                    AdsConstant.showBannerAll =
                        mFirebaseRemoteConfig.getBoolean("show_banner_all")
                    AdsConstant.showNativeSmallAll =
                        mFirebaseRemoteConfig.getBoolean("show_native_small_all")
                    AdsConstant.showOpenResume =
                        mFirebaseRemoteConfig.getBoolean("show_open_resume")
                    AdsConstant.showInterAll =
                        mFirebaseRemoteConfig.getBoolean("show_inter_all")
                    AdsConstant.newUILfo =
                        mFirebaseRemoteConfig.getBoolean("new_lfo_ui")
                    AdsConstant.useNativeLanguage11High =  mFirebaseRemoteConfig.getBoolean("use_native_language_1_1_high")
                    AdsConstant.useNativeLanguage12High =  mFirebaseRemoteConfig.getBoolean("use_native_language_1_2_high")
                    AdsConstant.useNativeOnboardFullscreen11High =  mFirebaseRemoteConfig.getBoolean("use_native_onboard_fullscreen_1_1_high")
                    AdsConstant.useNativeOnboardFullscreen12High = mFirebaseRemoteConfig.getBoolean("use_native_onboard_fullscreen_1_2_high")
                    AdsConstant.useInterSplashHigh = mFirebaseRemoteConfig.getBoolean("use_inter_splash_high")
                    AdsConstant.timeApplyLfo = mFirebaseRemoteConfig.getLong("time_apply_lfo")
                    AdsConstant.timeLoadingOnboard = mFirebaseRemoteConfig.getLong("time_loading_onboard")
                    AdsConstant.interIntervalTime = mFirebaseRemoteConfig.getLong("inter_interval_time")

                    onComplete(true)
                } else {
                    Timber.w(task.exception, "Remote config fetch failed")
                    onComplete(false)
                }
            }

    }

    private fun checkRemoteConfigResult() {
        // Which ads to preload, and whether the progress bar gates the hand-off, both
        // depend on whether the language step is still ahead — so wait for that flag.
        lifecycleScope.launch {
            splashViewModel.awaitLoaded()
            onRemoteConfigReady()
        }
    }

    private fun onRemoteConfigReady() {
        preloadNativeLanguage()
        if (AdsConstant.showInterSplash) {
            interAdSplashHelper.requestAds(InterstitialAdParam.Request)
        } else {
            interRequestCompleteFlow.value = true
        }

        if (AdsConstant.showBannerSplash) {
            showBanner = true
            bannerAdHelper.setBannerContentView(bannerContainer)
            bannerAdHelper.requestAds(BannerAdParam.Request)
        } else {
            showBanner = false
            bannerRequestCompleteFlow.value = true
        }
        InterAdsManager.configInterAds(this)
        bannerRequestCompleteFlow.combine(interRequestCompleteFlow) { requestBannerComplete, requestInterComplete ->
            requestInterComplete && requestBannerComplete
        }
            .combine(progressView.progressFlow) { adsComplete, progress ->
                if (!isStartLanguageShowed) {
                    adsComplete && progress >= 90
                } else {
                    adsComplete
                }
            }
            .filter { it }
            .onEach {
                interAdSplashHelper.interstitialAdValue?.let {
                    interAdSplashHelper.requestAds(InterstitialAdParam.Show(it))
                } ?: run {
                    startNextAct()
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun preloadNativeLanguage() {
        if (!isStartLanguageShowed) {
            AdsConstant.requestNativeLFO1(this)
            AdsConstant.requestNativeLFO2(this)
        }

    }
}
