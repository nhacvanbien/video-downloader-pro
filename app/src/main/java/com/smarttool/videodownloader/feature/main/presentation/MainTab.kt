package com.smarttool.videodownloader.feature.main.presentation

import com.smarttool.videodownloader.android.R

/** The three bottom-navigation destinations, in display order. */
enum class MainTab(
    val labelRes: Int,
    val selectedIconRes: Int,
    val normalIconRes: Int,
) {
    Browser(
        R.string.string_browser,
        R.drawable.ic_browser_navigation_selected,
        R.drawable.ic_browser_navigation_normal,
    ),
    Downloads(
        R.string.string_downloads,
        R.drawable.ic_downloaded_navigation_selected,
        R.drawable.ic_downloaded_navigation_normal,
    ),
    Settings(
        R.string.string_settings,
        R.drawable.ic_settings_navigation_selected,
        R.drawable.ic_settings_navigation_normal,
    ),
}
