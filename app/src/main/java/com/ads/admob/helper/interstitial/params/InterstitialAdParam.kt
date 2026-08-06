package com.ads.admob.helper.interstitial.params

import com.ads.admob.data.ContentAd

sealed class InterstitialAdParam {
    data object Request : InterstitialAdParam()
    class Show(val ad: ContentAd) : InterstitialAdParam()
}
