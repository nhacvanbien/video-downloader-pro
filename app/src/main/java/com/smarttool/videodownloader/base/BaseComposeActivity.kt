package com.smarttool.videodownloader.base

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.smarttool.videodownloader.core.SystemUtil

/**
 * Compose counterpart of [BaseActivity]: applies the same immersive-fullscreen window
 * setup and locale handling, so migrated screens keep drawing edge-to-edge with the
 * system bars hidden exactly as the View-based screens did.
 */
abstract class BaseComposeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        SystemUtil.setLocale(this)

        val flags = (
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        window.decorView.systemUiVisibility = flags
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                window.decorView.systemUiVisibility = flags
            }
        }

        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )
    }
}
