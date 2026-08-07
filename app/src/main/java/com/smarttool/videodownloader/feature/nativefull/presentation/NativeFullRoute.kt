package com.smarttool.videodownloader.feature.nativefull.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.smarttool.videodownloader.core.ads.InterAdsManager

/** Back is blocked so the ad cannot be dismissed without going through [onClose]. */
@Composable
fun NativeFullRoute(onClose: () -> Unit) {
    BackHandler { }

    NativeFullScreen(
        onClose = {
            InterAdsManager.onNativeFullActivityFinished()
            onClose()
        },
    )
}
