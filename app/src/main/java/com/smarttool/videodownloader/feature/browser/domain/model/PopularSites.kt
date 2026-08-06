package com.smarttool.videodownloader.feature.browser.domain.model

import com.smarttool.videodownloader.android.R

/** Shortcut tiles on the browser home, in the order the View layout listed them. */
val POPULAR_SITES = listOf(
    PopularSite(R.string.string_google, R.drawable.ic_google, "https://www.google.com"),
    PopularSite(R.string.string_facebook, R.drawable.ic_facebook, "https://www.facebook.com/watch"),
    PopularSite(R.string.string_instagram, R.drawable.ic_instagram, "https://www.instagram.com"),
    PopularSite(R.string.string_tiktok, R.drawable.ic_tiktok, "https://www.tiktok.com"),
    PopularSite(R.string.string_vimeo, R.drawable.ic_vimeo, "https://vimeo.com"),
    PopularSite(R.string.string_dailymotion, R.drawable.ic_dailymotion, "https://www.dailymotion.com"),
    PopularSite(R.string.string_pinterest, R.drawable.ic_pinterest, "https://www.pinterest.com/"),
    PopularSite(R.string.string_x, R.drawable.ic_x, "https://x.com/"),
)
