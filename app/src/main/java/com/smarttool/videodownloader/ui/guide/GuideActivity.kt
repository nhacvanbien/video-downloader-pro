package com.smarttool.videodownloader.ui.guide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.guide.presentation.GuideScreen
import com.smarttool.videodownloader.core.SystemUtil
import com.smarttool.videodownloader.core.ads.showInterAll

class GuideActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isHomeGuide = intent.getStringExtra(EXTRA_OPEN) == OPEN_HOME

        setContent {
            AppTheme {
                GuideScreen(
                    isHomeGuide = isHomeGuide,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
    }

    override fun onBackPressed() {
        showInterAll {
            super.onBackPressed()
        }
    }

    companion object {
        private const val EXTRA_OPEN = "open"
        private const val OPEN_HOME = "home"

        fun newIntent(context: Context): Intent {
            return Intent(context, GuideActivity::class.java)
        }
    }
}
