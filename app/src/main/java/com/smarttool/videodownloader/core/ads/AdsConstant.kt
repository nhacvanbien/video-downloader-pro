package com.smarttool.videodownloader.core.ads

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.ads.admob.admob.TevoAdmobFactory
import com.ads.admob.data.ContentAd
import com.ads.admob.listener.NativeAdCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.smarttool.videodownloader.android.BuildConfig

object AdsConstant {
    var showInterSplash = true
    var showBannerSplash = true
    var showNativeLanguage1_1 = true
    var showNativeLanguage1_2 = true
    var showNativeOnboardFullscreen1_1 = true
    var showNativeOnboardFullscreen1_2 = true
    var showNativePermission = true
    var showNativeHome = true
    var showBannerAll = true
    var showNativeSmallAll = true
    var showOpenResume = true
    var showInterAll = true

    var useNativeLanguage11High = true
    var useNativeLanguage12High = true
    var useNativeOnboardFullscreen11High = true
    var useNativeOnboardFullscreen12High = true
    var useInterSplashHigh = true

    var timeApplyLfo = 4L // seconds
    var timeLoadingOnboard = 2L // seconds
    var interIntervalTime = 20L // seconds

    var nativeAdsLanguage1_1: MutableLiveData<ContentAd?> = MutableLiveData()
    var nativeAdsLanguage1_2: MutableLiveData<ContentAd?> = MutableLiveData()
    var nativeAdsOnboardFullscreen1_1: MutableLiveData<ContentAd?> = MutableLiveData()
    var nativeAdsOnboardFullscreen1_2: MutableLiveData<ContentAd?> = MutableLiveData()
    var nativeAdsPermission: MutableLiveData<ContentAd?> = MutableLiveData()
    var nativeAdsSmallAll: MutableLiveData<ContentAd?> = MutableLiveData()
    var lastShownInter = 0L

    var newUILfo = true
    fun requestNativeLFO1(context: Context) {
        if (!showNativeLanguage1_1) return nativeAdsLanguage1_1.postValue(null)
        requestNativeAlternate(
            context,
            idAdPriorityAd = BuildConfig.NATIVE_LANGUAGE_1_1_HIGH,
            idAdAllPrice = BuildConfig.NATIVE_LANGUAGE_1_1,
            usePriorityAd = useNativeLanguage11High,
            onAdLoaded = { nativeAd ->
                nativeAdsLanguage1_1.postValue(nativeAd)
            },
            onFailedToLoad = {
                nativeAdsLanguage1_1.postValue(null)
            },
            onAdImpression = {},
            onClick = {},
            adPlacement = "native_language_1_1",
            adPlacementPriority = "native_language_1_1_high"
        )
    }

    fun requestNativeLFO2(context: Context) {
        if (!showNativeLanguage1_2) return nativeAdsLanguage1_2.postValue(null)
        requestNativeAlternate(
            context,
            idAdPriorityAd = BuildConfig.NATIVE_LANGUAGE_1_2_HIGH,
            idAdAllPrice = BuildConfig.NATIVE_LANGUAGE_1_2,
            usePriorityAd = useNativeLanguage12High,
            onAdLoaded = { nativeAd ->
                nativeAdsLanguage1_2.postValue(nativeAd)
            },
            onFailedToLoad = {
                nativeAdsLanguage1_2.postValue(null)
            },
            onAdImpression = {},
            onClick = {},
            adPlacement = "native_language_1_2",
            adPlacementPriority = "native_language_1_2_high"
        )
    }

    fun requestNativeFullscreen1(context: Context) {
        if (!showNativeOnboardFullscreen1_1) return nativeAdsOnboardFullscreen1_1.postValue(null)
        requestNativeAlternate(
            context,
            idAdPriorityAd = BuildConfig.NATIVE_ONBOARD_FULLSCREEN_1_1_HIGH,
            idAdAllPrice = BuildConfig.NATIVE_ONBOARD_FULLSCREEN_1_1,
            usePriorityAd = useNativeOnboardFullscreen11High,
            onAdLoaded = { nativeAd ->
                nativeAdsOnboardFullscreen1_1.postValue(nativeAd)
            },
            onFailedToLoad = {
                nativeAdsOnboardFullscreen1_1.postValue(null)
            },
            onAdImpression = {},
            onClick = {},
            adPlacement = "native_onboarding_fullscreen_1_1",
            adPlacementPriority = "native_onboarding_fullscreen_1_1_high"
        )
    }

    fun requestNativeFullscreen2(context: Context) {
        if (!showNativeOnboardFullscreen1_2) return nativeAdsOnboardFullscreen1_2.postValue(null)
        requestNativeAlternate(
            context,
            idAdPriorityAd = BuildConfig.NATIVE_ONBOARD_FULLSCREEN_1_2_HIGH,
            idAdAllPrice = BuildConfig.NATIVE_ONBOARD_FULLSCREEN_1_2,
            usePriorityAd = useNativeOnboardFullscreen12High,
            onAdLoaded = { nativeAd ->
                nativeAdsOnboardFullscreen1_2.postValue(nativeAd)
            },
            onFailedToLoad = {
                nativeAdsOnboardFullscreen1_2.postValue(null)
            },
            onAdImpression = {},
            onClick = {},
            adPlacement = "native_onboarding_fullscreen_1_2",
            adPlacementPriority = "native_onboarding_fullscreen_1_2_high"
        )
    }

    fun requestNativePermission(context: Context) {
        if (!showNativePermission) return nativeAdsPermission.postValue(null)
        requestNativeAd(
            context,
            idAd = BuildConfig.NATIVE_PERMISSION,
            onAdLoaded = { nativeAd ->
                nativeAdsPermission.postValue(nativeAd)
            },
            onFailedToLoad = {
                nativeAdsPermission.postValue(null)
            },
            onAdImpression = {},
            onClick = {},
            adPlacement = "native_permission",
        )
    }

}

private fun requestNativeAlternate(
    context: Context,
    idAdPriorityAd: String,
    idAdAllPrice: String,
    usePriorityAd: Boolean,
    onAdLoaded: (ContentAd) -> Unit,
    onFailedToLoad: () -> Unit,
    onAdImpression: () -> Unit = {},
    onClick: () -> Unit,
    adPlacementPriority: String,
    adPlacement: String,
) {
    if (usePriorityAd) {
        requestNativeAd(
            context,
            idAdPriorityAd,
            onAdLoaded = { nativeAd ->
                onAdLoaded(nativeAd)
            },
            onFailedToLoad = {
                requestNativeAd(
                    context,
                    idAdAllPrice,
                    onAdLoaded = { nativeAd ->
                        onAdLoaded(nativeAd)
                    },
                    onFailedToLoad = {
                        onFailedToLoad()
                    },
                    onAdImpression = onAdImpression,
                    onClick = onClick,
                    adPlacement = adPlacement,
                )
            },
            onClick = onClick,
            adPlacement = adPlacementPriority,
        )
    } else {
        requestNativeAd(
            context,
            idAdAllPrice,
            onAdLoaded = { nativeAd ->
                onAdLoaded(nativeAd)
            },
            onFailedToLoad = {
                onFailedToLoad()
            },
            onAdImpression = onAdImpression,
            onClick = onClick,
            adPlacement = adPlacement,
        )
    }

}

private fun requestNativeAd(
    context: Context,
    idAd: String,
    onAdLoaded: (ContentAd) -> Unit,
    onFailedToLoad: () -> Unit,
    onAdImpression: () -> Unit = {},
    onClick: () -> Unit,
    adPlacement: String,
) {
    TevoAdmobFactory.requestNativeAd(
        context,
        idAd,
        adPlacement,
        object : NativeAdCallback {
            override fun populateNativeAd() {}

            override fun onAdLoaded(data: ContentAd) {
                onAdLoaded(data)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                onFailedToLoad()
            }

            override fun onAdClicked() {
                onClick()
            }

            override fun onAdImpression() {
                onAdImpression()
            }

            override fun onAdFailedToShow(adError: AdError) {}
        },
    )
}
