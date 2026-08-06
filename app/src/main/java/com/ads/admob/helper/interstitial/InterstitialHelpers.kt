package com.ads.admob.helper.interstitial

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.data.ContentAd
import com.ads.admob.listener.InterstitialAdCallback
import com.ads.admob.listener.InterstitialAdRequestCallBack
import com.ads.admob.listener.InterstitialAdShowCallBack

data class InterstitialAdConfig(
    val idAds: String,
    val canShowAds: Boolean,
    val canReloadAds: Boolean,
    val adPlacement: String,
)

data class InterstitialAdSplashConfig(
    val idAds: String,
    val idAdsPriority: String? = null,
    val canShowAds: Boolean,
    val canReloadAds: Boolean,
    val timeDelay: Long,
    val timeOut: Long,
    val showReady: Boolean,
    val adPlacement: String,
)

class InterstitialAdSplashHelper(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val config: InterstitialAdSplashConfig,
    private val showAfterLoaded: Boolean,
) {
    var interstitialAdValue: ContentAd? = null
    private var callback: InterstitialAdCallback? = null

    fun registerAdListener(callback: InterstitialAdCallback): InterstitialAdSplashHelper {
        this.callback = callback
        return this
    }

    fun requestAds(param: Any): InterstitialAdSplashHelper {
        return this
    }
}

class InterstitialAdsHelper private constructor(private val placement: String) {
    companion object {
        private val instances = mutableMapOf<String, InterstitialAdsHelper>()

        fun getInstance(placement: String): InterstitialAdsHelper =
            instances.getOrPut(placement) { InterstitialAdsHelper(placement) }
    }

    fun setInterstitialAdConfig(config: InterstitialAdConfig): InterstitialAdsHelper = this

    fun requestInterAds(context: Context, callback: InterstitialAdRequestCallBack) {
    }

    fun forceShowInterstitial(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        callback: InterstitialAdShowCallBack,
    ) {
        callback.onNextAction()
    }
}
