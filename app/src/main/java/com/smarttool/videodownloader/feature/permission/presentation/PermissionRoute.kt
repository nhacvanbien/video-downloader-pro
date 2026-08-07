package com.smarttool.videodownloader.feature.permission.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.smarttool.videodownloader.VideoDownloaderApplication
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.permission.MediaPermissionChecker
import com.smarttool.videodownloader.helper.PreferenceHelper
import org.koin.compose.koinInject

/**
 * Onboarding permission step. Skipping is allowed — the grants are re-requested from
 * the download flow — so both buttons lead to the same place.
 */
@Composable
fun PermissionRoute(onContinue: () -> Unit) {
    val preferenceHelper: PreferenceHelper = koinInject()
    val checker: MediaPermissionChecker = koinInject()
    val context = LocalContext.current

    var state by remember {
        mutableStateOf(
            PermissionUiState(
                showNotificationRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            ),
        )
    }

    val refresh = {
        state = state.copy(
            storageGranted = checker.hasStorage(),
            notificationGranted = checker.hasNotification(),
        )
    }

    val showGoToSettings = {
        val dialog = AlertDialog.Builder(context).create()
        dialog.setCancelable(false)
        dialog.setMessage(
            context.getString(R.string.string_you_need_to_enable_permission_to_use_this_features),
        )
        dialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            context.getString(R.string.go_to_setting),
        ) { _, _ ->
            VideoDownloaderApplication.instance.appResumeAdHelper.setDisableAppResumeOnScreen()
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                },
            )
            dialog.dismiss()
        }
        dialog.show()
    }

    val requestNotification = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        refresh()
        if (!granted) showGoToSettings()
    }

    val requestMediaVideo = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        refresh()
        if (!granted) showGoToSettings()
    }

    // Tiramisu+ splits media access in two, so images are requested before video.
    val requestMediaImages = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        requestMediaVideo.launch(Manifest.permission.READ_MEDIA_VIDEO)
    }

    val requestLegacyStorage = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        refresh()
        if (results.values.any { !it }) showGoToSettings()
    }

    // Also re-runs after the system settings round trip, which is the only way a grant
    // can change without one of the launchers above reporting it.
    LifecycleResumeEffect(Unit) {
        refresh()
        VideoDownloaderApplication.instance.appResumeAdHelper.setEnableAppResumeOnScreen()
        onPauseOrDispose { }
    }

    val finishOnboarding = {
        preferenceHelper.hidePermission()
        onContinue()
    }

    PermissionScreen(
        state = state,
        onSkip = finishOnboarding,
        onRequestStorage = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestMediaImages.launch(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                requestLegacyStorage.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                    ),
                )
            }
        },
        onRequestNotification = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onContinue = finishOnboarding,
    )
}
