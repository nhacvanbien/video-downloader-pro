package com.smarttool.videodownloader.base

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.smarttool.videodownloader.core.SystemUtil

/**
 * Applies the immersive-fullscreen window setup and locale handling the deleted
 * View-based `BaseActivity` used to, so screens keep drawing edge-to-edge with the
 * system bars hidden exactly as they did before the Compose migration.
 */
abstract class BaseComposeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        SystemUtil.setLocale(this)

//        val flags = (
//            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                or View.SYSTEM_UI_FLAG_FULLSCREEN
//                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//            )
//
//        window.decorView.systemUiVisibility = flags
//        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
//            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
//                window.decorView.systemUiVisibility = flags
//            }
//        }

        super.onCreate(savedInstanceState)

//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            WindowManager.LayoutParams.FLAG_FULLSCREEN,
//        )
    }
}
