package com.ads.admob.helper.banner

import android.app.Activity
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.listener.BannerAdCallBack

data class BannerAdConfig(
    val idAds: String,
    val canShowAds: Boolean,
    val canReloadAds: Boolean,
    val adPlacement: String,
)

class BannerAdHelper(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val config: BannerAdConfig,
) {
    var listener: BannerAdCallBack? = null
        private set

    fun setBannerContentView(viewGroup: ViewGroup): BannerAdHelper = this

    fun requestAds(param: Any): BannerAdHelper = this

    fun registerAdListener(callback: BannerAdCallBack): BannerAdHelper {
        listener = callback
        return this
    }
}
