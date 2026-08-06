/*
 * Copyright (c) 2025. Tevo Global Limited
 *
 * This software and all accompanying documentation is the sole property of
 * Tevo Global Limited and is protected by copyright law and international treaties.
 *
 * Unauthorized copying, distribution, or reproduction of this software, or any
 * portion of it, is strictly prohibited. The software is licensed to you solely for
 * your personal use and may not be used for commercial purposes without
 * a separate license agreement.
 *
 * You may not modify, reverse engineer, decompile, or disassemble this software.
 * You are not permitted to remove or alter any copyright notices or proprietary
 * legends from the software.
 *
 * All rights not expressly granted herein are reserved by Tevo Global Limited.
 *
 * Contact information: hello@tevo.app
 */

package com.smarttool.videodownloader.ui.language

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.language.presentation.LanguageAppliedScreen
import com.smarttool.videodownloader.ui.intro.IntroActivity
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.SystemUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LanguageAppliedActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val languageName = intent.getStringExtra(EXTRA_LANGUAGE_NAME) ?: "English"
        val languageIcon = intent.getIntExtra(EXTRA_LANGUAGE_ICON, 0)

        setContent {
            AppTheme {
                LanguageAppliedScreen(
                    languageName = languageName,
                    languageIconRes = languageIcon,
                )
            }
        }

        lifecycleScope.launch {
            delay(AdsConstant.timeApplyLfo * 1000L)
            navigateToIntro()
        }
    }

    private fun navigateToIntro() {
        startActivity(
            IntroActivity.newIntent(this).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
        finish()
    }

    companion object {
        private const val EXTRA_LANGUAGE_NAME = "extra_language_name"
        private const val EXTRA_LANGUAGE_ICON = "extra_language_icon"

        fun newIntent(context: Context, languageName: String, languageIcon: Int): Intent {
            return Intent(context, LanguageAppliedActivity::class.java).apply {
                putExtra(EXTRA_LANGUAGE_NAME, languageName)
                putExtra(EXTRA_LANGUAGE_ICON, languageIcon)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }
    }
}
