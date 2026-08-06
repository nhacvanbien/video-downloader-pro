package com.ads.admob.helper.adnative

import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.facebook.shimmer.ShimmerFrameLayout

data class NativeAdConfig(
    val idAds: String,
    val canShowAds: Boolean,
    val canReloadAds: Boolean,
    val layoutId: Int,
    val adPlacement: String,
)

class NativeAdHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val config: NativeAdConfig,
) {
    fun setNativeContentView(viewGroup: ViewGroup): NativeAdHelper = this

    fun setShimmerLayoutView(shimmerFrameLayout: ShimmerFrameLayout): NativeAdHelper = this

    fun requestAds(param: Any): NativeAdHelper = this
}
