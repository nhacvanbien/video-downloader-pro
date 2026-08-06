package com.smarttool.videodownloader.ui.disclaimers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.smarttool.videodownloader.MainActivity
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.disclaimers.presentation.DisclaimersScreen
import com.smarttool.videodownloader.core.SystemUtil

class DisclaimersActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                DisclaimersScreen(onAgree = ::openMain)
            }
        }
    }

    private fun openMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, DisclaimersActivity::class.java)
        }
    }
}
