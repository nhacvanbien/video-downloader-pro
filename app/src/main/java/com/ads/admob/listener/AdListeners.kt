package com.ads.admob.listener

import com.ads.admob.data.ContentAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

interface BannerAdCallBack {
    fun onAdImpression() {}
    fun onAdClicked() {}
    fun onAdFailedToLoad(loadAdError: LoadAdError) {}
    fun onAdFailedToShow(adError: AdError) {}
    fun onAdLoaded(data: ContentAd) {}
}

interface NativeAdCallback {
    fun populateNativeAd() {}
    fun onAdLoaded(data: ContentAd) {}
    fun onAdFailedToLoad(loadAdError: LoadAdError) {}
    fun onAdClicked() {}
    fun onAdImpression() {}
    fun onAdFailedToShow(adError: AdError) {}
}

interface InterstitialAdCallback {
    fun onNextAction() {}
    fun onAdClose() {}
    fun onInterstitialShow() {}
    fun onAdLoaded(data: ContentAd) {}
    fun onAdFailedToLoad(loadAdError: LoadAdError) {}
    fun onAdClicked() {}
    fun onAdImpression() {}
    fun onAdFailedToShow(adError: AdError) {}
}

interface InterstitialAdRequestCallBack {
    fun onAdFailedToLoad(loadAdError: LoadAdError) {}
    fun onAdLoaded(data: ContentAd) {}
}

interface InterstitialAdShowCallBack {
    fun onAdClicked() {}
    fun onAdClose() {}
    fun onAdFailedToShow(adError: AdError) {}
    fun onAdImpression() {}
    fun onInterstitialShow() {}
    fun onNextAction() {}
}
