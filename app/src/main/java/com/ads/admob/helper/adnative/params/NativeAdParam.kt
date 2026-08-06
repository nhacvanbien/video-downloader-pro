package com.ads.admob.helper.adnative.params

import com.ads.admob.data.ContentAd

sealed class NativeAdParam {
    class Ready(val ad: ContentAd) : NativeAdParam()

    class Request private constructor() : NativeAdParam() {
        companion object {
            fun create(): Request = Request()
        }
    }
}
