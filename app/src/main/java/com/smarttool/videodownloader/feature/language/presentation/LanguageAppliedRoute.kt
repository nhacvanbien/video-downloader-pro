package com.smarttool.videodownloader.feature.language.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.smarttool.videodownloader.feature.language.domain.TIME_APPLY_LFO_SECONDS
import kotlinx.coroutines.delay

/**
 * Confirmation beat between the language picker and onboarding. It advances on its own
 * after a short delay and cannot be backed out of.
 */
@Composable
fun LanguageAppliedRoute(
    languageName: String,
    languageIconRes: Int,
    onContinue: () -> Unit,
) {
    BackHandler { }

    LaunchedEffect(Unit) {
        delay(TIME_APPLY_LFO_SECONDS * 1000L)
        onContinue()
    }

    LanguageAppliedScreen(
        languageName = languageName,
        languageIconRes = languageIconRes,
    )
}
