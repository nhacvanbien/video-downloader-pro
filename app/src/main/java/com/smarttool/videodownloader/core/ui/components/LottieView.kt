package com.smarttool.videodownloader.core.ui.components

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

/** Bridges the View-based Lottie player, which has no Compose artifact in this project. */
@Composable
fun LottieView(
    @RawRes rawRes: Int,
    modifier: Modifier = Modifier,
    loop: Boolean = true,
) {
    AndroidView(
        factory = { context ->
            LottieAnimationView(context).apply {
                setAnimation(rawRes)
                repeatCount = if (loop) LottieDrawable.INFINITE else 0
                playAnimation()
            }
        },
        modifier = modifier,
    )
}
