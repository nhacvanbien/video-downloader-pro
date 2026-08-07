package com.smarttool.videodownloader.feature.disclaimers.presentation

import androidx.compose.runtime.Composable

/** Last onboarding step; agreeing is the only way forward. */
@Composable
fun DisclaimersRoute(onAgree: () -> Unit) {
    DisclaimersScreen(onAgree = onAgree)
}
