package com.smarttool.videodownloader.core.ads

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.interstitial.InterstitialAdConfig
import com.ads.admob.helper.interstitial.test.InterstitialAdsHelper
import com.ads.admob.listener.InterstitialAdRequestCallBack
import com.ads.admob.listener.InterstitialAdShowCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.smarttool.videodownloader.android.BuildConfig
import timber.log.Timber

object InterAdsManager {
    const val INTER_ALL = "inter_all"

    /**
     * Opens the full-screen native ad. It used to be `startActivity(NativeFullActivity)`;
     * now that it is a destination in the nav graph, the host Activity installs this on
     * create and clears it on destroy. Null means there is no graph to navigate — the
     * caller falls through to its own action rather than dropping it.
     */
    var openNativeFull: (() -> Unit)? = null

    private var isShowNativeFull = false
    private var pendingAction: (() -> Unit)? = null

    fun configInterAds(context: Context?) {
        if (context == null) return
        InterstitialAdsHelper.getInstance(INTER_ALL).setInterstitialAdConfig(
            InterstitialAdConfig(
                idAds = BuildConfig.INTER_ALL,
                canShowAds = AdsConstant.showInterAll,
                canReloadAds = false,
                adPlacement = INTER_ALL,
            )
        )
    }

    fun requestInter(context: Context?, adPlacement: String) {
        if (context == null) return
        InterstitialAdsHelper.getInstance(adPlacement)
            .requestInterAds(context, object : InterstitialAdRequestCallBack {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                }

                override fun onAdLoaded(data: ContentAd) {
                }
            })
    }

    fun onNativeFullActivityFinished() {
        isShowNativeFull = true
        pendingAction?.invoke()
        pendingAction = null
    }

    fun showInterAll(
        activity: FragmentActivity,
        onAction: () -> Unit
    ) {
        if (!activity.isNetwork() || (isShowNativeFull && !CheckTimeShowAdsInter.isTimeShow)) {
            onAction.invoke()
            return
        }
        if (!isShowNativeFull && !CheckTimeShowAdsInter.isTimeShow) {
            val open = openNativeFull

            if (open == null) {
                onAction.invoke()
                return
            }

            // The ad destination calls back into onNativeFullActivityFinished, which is
            // what runs the pending action once it closes.
            pendingAction = onAction
            open()
            return
        }

        activity.let {
            InterstitialAdsHelper.getInstance(INTER_ALL)
                .forceShowInterstitial(
                    activity,
                    activity,
                    object : InterstitialAdShowCallBack {
                        override fun onAdClicked() {

                        }

                        override fun onAdClose() {
                            isShowNativeFull = false
                            CheckTimeShowAdsInter.logShowed()
                            requestInter(
                                activity,
                                INTER_ALL
                            )
                            onAction()
                        }

                        override fun onAdFailedToShow(adError: AdError) {
                            Timber.e("onAdFailedToShow: ${adError.message}")
                        }

                        override fun onAdImpression() {
                        }

                        override fun onInterstitialShow() {
                        }

                        override fun onNextAction() {
                            onAction()
                        }

                    })
        }
    }
}

fun FragmentActivity.showInterAll(onAction: () -> Unit) {
    InterAdsManager.showInterAll(this) {
        onAction()
    }
}

fun Fragment.showInterAll(onAction: () -> Unit) {
    val activity = this.activity
    if (activity != null) {
        InterAdsManager.showInterAll(activity) {
            onAction()
        }
    }
}

fun View.setOnClickListenerWithShowInterAd(activity: FragmentActivity?, action: () -> Unit) {
    if (activity == null) return
    setOnClickListener {
        activity.showInterAll(action)
    }
}

object CheckTimeShowAdsInter {
    private var lastShow: Long = 0
    val isTimeShow: Boolean
        get() {
            var isTimeShow = true

            var time = if (AdsConstant.interIntervalTime > 20) {
                AdsConstant.interIntervalTime
            } else {
                20
            }

            if (lastShow == 0L) {
                lastShow = Long.MIN_VALUE
            }
            if (lastShow > System.currentTimeMillis() - time * 1000) {
                isTimeShow = false
            }
            return isTimeShow
        }

    fun logShowed() {
        lastShow = System.currentTimeMillis()
    }
}

fun Context.isNetwork(): Boolean {
    val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.activeNetworkInfo != null && cm.activeNetworkInfo?.isConnected == true
}