package com.smarttool.videodownloader.feature.permission.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import org.koin.androidx.compose.koinViewModel

/**
 * Onboarding permission step. Skipping is allowed — the grants are re-requested from
 * the download flow — so both buttons lead to the same place.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun PermissionRoute(onContinue: () -> Unit) {
    val viewModel: PermissionViewModel = koinViewModel()
    val context = LocalContext.current

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PermissionContract.Effect.Completed -> onContinue()
            }
        }
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
        viewModel.onEvent(PermissionContract.Event.RefreshGrants)
        if (!granted) showGoToSettings()
    }

    val requestMediaVideo = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onEvent(PermissionContract.Event.RefreshGrants)
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
        viewModel.onEvent(PermissionContract.Event.RefreshGrants)
        if (results.values.any { !it }) showGoToSettings()
    }

    // Also re-runs after the system settings round trip, which is the only way a grant
    // can change without one of the launchers above reporting it.
    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(PermissionContract.Event.RefreshGrants)
        onPauseOrDispose { }
    }

    PermissionScreen(
        state = state,
        onSkip = { viewModel.onEvent(PermissionContract.Event.Complete) },
        onRequestStorage = {
            requestMediaImages.launch(Manifest.permission.READ_MEDIA_IMAGES)
        },
        onRequestNotification = {
            requestNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onContinue = { viewModel.onEvent(PermissionContract.Event.Complete) },
    )
}
