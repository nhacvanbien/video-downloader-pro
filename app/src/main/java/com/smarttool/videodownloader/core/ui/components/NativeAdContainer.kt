package com.smarttool.videodownloader.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.ads.admob.helper.adnative.NativeAdConfig
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.smarttool.videodownloader.android.databinding.LayoutNativeAdContainerBinding
import com.smarttool.videodownloader.android.databinding.LayoutNativeAdFullContainerBinding

/**
 * Compose bridge for the View-based native ad SDK (com.ads.admob), which has no
 * Compose surface of its own. Reused by every ad-bearing screen as it's migrated.
 */
@Composable
fun NativeAdContainer(
    adUnitId: String,
    layoutRes: Int,
    adPlacement: String,
    canShowAds: Boolean,
    canReloadAds: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.findComponentActivity()

    AndroidViewBinding(
        factory = { inflater, parent, attachToParent ->
            LayoutNativeAdContainerBinding.inflate(inflater, parent, attachToParent)
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        val config = NativeAdConfig(
            idAds = adUnitId,
            canShowAds = canShowAds,
            canReloadAds = canReloadAds,
            layoutId = layoutRes,
            adPlacement = adPlacement,
        )
        NativeAdHelper(activity, activity, config)
            .setNativeContentView(frAds)
            .setShimmerLayoutView(shimmerContainerNative.shimmerSmall)
            .requestAds(NativeAdParam.Request.create())
    }
}

/** Full-screen variant of [NativeAdContainer], for the fullscreen/onboarding ad slots. */
@Composable
fun NativeAdFullContainer(
    adUnitId: String,
    layoutRes: Int,
    adPlacement: String,
    canShowAds: Boolean,
    canReloadAds: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.findComponentActivity()

    AndroidViewBinding(
        factory = { inflater, parent, attachToParent ->
            LayoutNativeAdFullContainerBinding.inflate(inflater, parent, attachToParent)
        },
        modifier = modifier,
    ) {
        val config = NativeAdConfig(
            idAds = adUnitId,
            canShowAds = canShowAds,
            canReloadAds = canReloadAds,
            layoutId = layoutRes,
            adPlacement = adPlacement,
        )
        NativeAdHelper(activity, activity, config)
            .setNativeContentView(frAds)
            .setShimmerLayoutView(shimmerFull.shimmerNativeLarge)
            .requestAds(NativeAdParam.Request.create())
    }
}
