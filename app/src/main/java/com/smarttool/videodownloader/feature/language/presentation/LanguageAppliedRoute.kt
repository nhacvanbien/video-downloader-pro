package com.smarttool.videodownloader.feature.language.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.smarttool.videodownloader.core.ads.AdsConstant
import kotlinx.coroutines.delay

/**
 * Confirmation beat between the language picker and onboarding. It advances on its own
 * after a remote-config delay and cannot be backed out of.
 */
@Composable
fun LanguageAppliedRoute(
    languageName: String,
    languageIconRes: Int,
    onContinue: () -> Unit,
) {
    BackHandler { }

    LaunchedEffect(Unit) {
        delay(AdsConstant.timeApplyLfo * 1000L)
        onContinue()
    }

    LanguageAppliedScreen(
        languageName = languageName,
        languageIconRes = languageIconRes,
    )
}
