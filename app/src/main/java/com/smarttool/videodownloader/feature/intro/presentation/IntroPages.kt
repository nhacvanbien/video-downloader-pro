package com.smarttool.videodownloader.feature.intro.presentation

import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ads.AdsConstant

/** One page of the onboarding pager: either a slide of content or a full-screen ad. */
sealed interface IntroPage {
    data class Slide(
        val imageRes: Int,
        val titleRes: Int,
        val descriptionRes: Int,
        val ctaRes: Int,
    ) : IntroPage

    data class FullscreenAd(
        val adUnitId: String,
        val adPlacement: String,
    ) : IntroPage
}

/**
 * Ad pages are interleaved only when remote config enables that slot, so the pager
 * length varies — the dots indicator counts slides only.
 */
fun buildIntroPages(): List<IntroPage> = buildList {
    add(
        IntroPage.Slide(
            imageRes = R.drawable.img_intro_slide_1,
            titleRes = R.string.string_title_intro_1,
            descriptionRes = R.string.string_des_intro_1,
            ctaRes = R.string.string_next,
        ),
    )

    if (AdsConstant.showNativeOnboardFullscreen1_1) {
        add(
            IntroPage.FullscreenAd(
                adUnitId = BuildConfig.NATIVE_ONBOARD_FULLSCREEN_1_1,
                adPlacement = "native_onboarding_fullscreen_1_1",
            ),
        )
    }

    add(
        IntroPage.Slide(
            imageRes = R.drawable.img_intro_slide_2,
            titleRes = R.string.string_title_intro_2,
            descriptionRes = R.string.string_des_intro_2,
            ctaRes = R.string.string_next,
        ),
    )

    if (AdsConstant.showNativeOnboardFullscreen1_2) {
        add(
            IntroPage.FullscreenAd(
                adUnitId = BuildConfig.NATIVE_ONBOARD_FULLSCREEN_1_2,
                adPlacement = "native_onboarding_fullscreen_1_2",
            ),
        )
    }

    add(
        IntroPage.Slide(
            imageRes = R.drawable.img_intro_slide_4,
            titleRes = R.string.string_title_intro_4,
            descriptionRes = R.string.string_des_intro_4,
            ctaRes = R.string.string_start,
        ),
    )
}
