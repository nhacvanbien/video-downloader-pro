package com.smarttool.videodownloader.feature.intro.presentation

import com.smarttool.videodownloader.android.R

/** One page of the onboarding pager. */
data class IntroPage(
    val imageRes: Int,
    val titleRes: Int,
    val descriptionRes: Int,
    val ctaRes: Int,
)

fun buildIntroPages(): List<IntroPage> = listOf(
    IntroPage(
        imageRes = R.drawable.img_intro_slide_1,
        titleRes = R.string.string_title_intro_1,
        descriptionRes = R.string.string_des_intro_1,
        ctaRes = R.string.string_next,
    ),
    IntroPage(
        imageRes = R.drawable.img_intro_slide_2,
        titleRes = R.string.string_title_intro_2,
        descriptionRes = R.string.string_des_intro_2,
        ctaRes = R.string.string_next,
    ),
    IntroPage(
        imageRes = R.drawable.img_intro_slide_4,
        titleRes = R.string.string_title_intro_4,
        descriptionRes = R.string.string_des_intro_4,
        ctaRes = R.string.string_start,
    ),
)
