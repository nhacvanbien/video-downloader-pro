package com.smarttool.videodownloader.core.ui

import android.view.View
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Shows/hides the system bars around fullscreen page content.
 *
 * Was `util/AppUtil`, which also carried keyboard helpers nothing called any more.
 */
class SystemUiController {

    fun hideSystemUI(window: Window, container: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, container).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun showSystemUI(window: Window, container: View) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, container)
            .show(WindowInsetsCompat.Type.systemBars())
    }
}
