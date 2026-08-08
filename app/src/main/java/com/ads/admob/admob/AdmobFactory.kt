package com.ads.admob.admob

import android.content.Context
import android.view.ViewGroup
import com.ads.admob.config.AdsConfig
import com.ads.admob.data.ContentAd
import com.ads.admob.listener.NativeAdCallback
import com.facebook.shimmer.ShimmerFrameLayout

object AdmobFactory {
    fun initAdmob(context: Context, adsConfig: AdsConfig) {
    }

    fun requestNativeAd(
        context: Context,
        idAd: String,
        adPlacement: String,
        callback: NativeAdCallback,
    ) {
    }

    fun populateNativeAdView(
        context: Context,
        data: ContentAd,
        layoutId: Int,
        container: ViewGroup,
        shimmer: ShimmerFrameLayout,
        callback: NativeAdCallback,
    ) {
    }
}
