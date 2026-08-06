package com.smarttool.videodownloader.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smarttool.videodownloader.VideoDownloaderApplication
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.permission.presentation.PermissionScreen
import com.smarttool.videodownloader.feature.permission.presentation.PermissionUiState
import com.smarttool.videodownloader.helper.PreferenceHelper
import com.smarttool.videodownloader.ui.disclaimers.DisclaimersActivity
import com.smarttool.videodownloader.core.SystemUtil
import org.koin.android.ext.android.inject

class PermissionActivity : BaseComposeActivity() {
    private val preferenceHelper: PreferenceHelper by inject()

    private var state by mutableStateOf(
        PermissionUiState(
            showNotificationRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refreshPermissionState()

        setContent {
            AppTheme {
                PermissionScreen(
                    state = state,
                    onSkip = ::finishOnboarding,
                    onRequestStorage = ::requestPermissionStorage,
                    onRequestNotification = ::requestPermissionNotification,
                    onContinue = ::finishOnboarding,
                )
            }
        }
    }

    private fun finishOnboarding() {
        preferenceHelper.hidePermission()
        startActivity(DisclaimersActivity.newIntent(this))
    }

    private fun refreshPermissionState() {
        state = state.copy(
            storageGranted = hasStoragePermission(),
            notificationGranted = hasNotificationPermission(),
        )
    }

    private fun hasStoragePermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(Manifest.permission.READ_MEDIA_VIDEO) &&
                isGranted(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            isGranted(Manifest.permission.READ_EXTERNAL_STORAGE) &&
                isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

    private fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Pre-Tiramisu notifications need no runtime grant, so treat as satisfied.
            true
        }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissionStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storageImageActivityResultLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ),
                REQUEST_CODE_STORAGE,
            )
        }
    }

    private fun requestPermissionNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATION,
            )
        }
    }

    private val storageImageActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            storageActivityResultLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
        }

    private val storageActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            refreshPermissionState()
            if (!granted) showGoToSettingsDialog()
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val granted = grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED

        refreshPermissionState()

        if (!granted && (requestCode == REQUEST_CODE_STORAGE || requestCode == REQUEST_CODE_NOTIFICATION)) {
            showGoToSettingsDialog()
        }
    }

    private fun showGoToSettingsDialog() {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setCancelable(false)
        alertDialog.setMessage(getString(R.string.string_you_need_to_enable_permission_to_use_this_features))
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            getString(R.string.go_to_setting),
        ) { _, _ ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)

            VideoDownloaderApplication.instance.appResumeAdHelper.setDisableAppResumeOnScreen()
            startActivityResult.launch(intent)

            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    val startActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        refreshPermissionState()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
        VideoDownloaderApplication.instance.appResumeAdHelper.setEnableAppResumeOnScreen()
    }

    companion object {
        private const val REQUEST_CODE_STORAGE = 3
        private const val REQUEST_CODE_NOTIFICATION = 4

        fun newIntent(context: Context): Intent {
            return Intent(context, PermissionActivity::class.java)
        }
    }
}
