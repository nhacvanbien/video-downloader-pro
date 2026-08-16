package com.smarttool.videodownloader.core.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.dialogs.setComposeContent
import com.smarttool.videodownloader.core.ui.theme.AppLocaleProvider
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.TextPrimary

/**
 * The "grant storage, then notifications" bottom sheet the browser and the processing
 * screen put in front of a download when a grant is missing.
 *
 * It was duplicated verbatim in `WebTabActivity` and `ProcessingFragment`; both now
 * share one instance owned by the host Activity. Result launchers go through
 * [AppCompatActivity.getActivityResultRegistry] directly rather than
 * `registerForActivityResult`, so the sheet can be constructed lazily — after the
 * Activity is already RESUMED — without tripping the registration lifecycle check.
 */
class StoragePermissionSheet(
    private val activity: AppCompatActivity,
    private val checker: MediaPermissionChecker,
) {

    private val handler = Handler(Looper.getMainLooper())

    private val dialog by lazy {
        BottomSheetDialog(activity, R.style.CustomAlertBottomSheet)
    }

    private var uiState by mutableStateOf(StoragePermissionUiState())
    private var pulseRunnable: Runnable? = null

    private val registry get() = activity.activityResultRegistry

    /** Tiramisu+ splits media access in two, so images are requested before video. */
    private val requestMediaVideo = registry.register(
        "storage_permission_sheet_media_video",
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        onRequestFinished(granted)
    }

    private val requestMediaImages = registry.register(
        "storage_permission_sheet_media_images",
        ActivityResultContracts.RequestPermission(),
    ) {
        requestMediaVideo.launch(Manifest.permission.READ_MEDIA_VIDEO)
    }

    private val requestLegacyStorage = registry.register(
        "storage_permission_sheet_legacy_storage",
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        onRequestFinished(results.values.all { it })
    }

    private val requestNotification = registry.register(
        "storage_permission_sheet_notification",
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        onRequestFinished(granted)
    }

    private val openAppSettings = registry.register(
        "storage_permission_sheet_app_settings",
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (checker.hasAll()) dialog.dismiss() else refreshButtons()
    }

    fun show() {
        dialog.setComposeContent(activity) {
            // This sheet is Activity-owned (constructed once, shown/dismissed repeatedly),
            // so its content lambda never sits inside the AppLocaleProvider that wraps
            // setContent() in MainActivity. Re-provide it here so a mid-session language
            // change reaches it too.
            AppLocaleProvider {
                StoragePermissionSheetContent(
                    state = uiState,
                    onClose = { dialog.dismiss() },
                    onStorageClick = { onStorageClicked() },
                    onNotificationClick = { onNotificationClicked() },
                )
            }
        }

        dialog.setCanceledOnTouchOutside(true)
        val behavior = dialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.addBottomSheetCallback(KeepExpandedCallback(behavior))

        refreshButtons()
        dialog.show()
    }

    private fun onStorageClicked() {
        restartPulse()
        requestStorage()
    }

    private fun onNotificationClicked() {
        restartPulse()
        requestNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requestStorage() {
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
    }

    private fun onRequestFinished(granted: Boolean) {
        if (!granted) {
            showSettingsDialog()
            return
        }

        if (checker.hasAll()) dialog.dismiss() else refreshButtons()
    }

    private fun refreshButtons() {
        val storageDone = checker.hasStorage()
        uiState = StoragePermissionUiState(
            storageGranted = storageDone,
            notificationGranted = checker.hasNotification(),
            storageEnabled = !storageDone,
            notificationEnabled = storageDone,
            pulseStorage = !storageDone,
        )
        restartPulse()
    }

    private fun restartPulse() {
        pulseRunnable?.let { handler.removeCallbacks(it) }

        val runnable = Runnable {
            uiState = uiState.copy(
                pulseStorage = uiState.storageEnabled,
                pulseNotification = uiState.notificationEnabled,
            )
        }

        pulseRunnable = runnable
        handler.postDelayed(runnable, PULSE_DELAY_MILLIS)
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.string_permission))
            .setMessage(activity.getString(R.string.permission_setting))
            .setPositiveButton(activity.getString(R.string.string_ok)) { _, _ ->
                launchAppSettings()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun launchAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }

        openAppSettings.launch(intent)
    }

    private companion object {
        const val PULSE_DELAY_MILLIS = 5000L
    }
}

private data class StoragePermissionUiState(
    val storageGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val storageEnabled: Boolean = true,
    val notificationEnabled: Boolean = false,
    val pulseStorage: Boolean = false,
    val pulseNotification: Boolean = false,
)

@Composable
private fun StoragePermissionSheetContent(
    state: StoragePermissionUiState,
    onClose: () -> Unit,
    onStorageClick: () -> Unit,
    onNotificationClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppWhite)
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.string_permission),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )

            Image(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onClose)
                    .padding(4.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            PermissionButton(
                label = stringResource(R.string.string_storage),
                enabled = state.storageEnabled,
                pulse = state.pulseStorage,
                onClick = onStorageClick,
                modifier = Modifier.weight(1f),
            )

            PermissionButton(
                label = stringResource(R.string.string_notification),
                enabled = state.notificationEnabled,
                pulse = state.pulseNotification,
                onClick = onNotificationClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
        }

        Image(
            painter = painterResource(R.drawable.img_permission),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
                .size(width = 160.dp, height = 120.dp),
        )

        PermissionItemRow(
            iconRes = if (state.storageGranted) R.drawable.ic_notification else R.drawable.ic_storage,
            label = stringResource(
                if (state.storageGranted) R.string.string_notification else R.string.string_storage
            ),
            switchEnabled = state.storageEnabled,
            onToggle = if (!state.storageGranted) onStorageClick else onNotificationClick,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun PermissionButton(
    label: String,
    enabled: Boolean,
    pulse: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = if (enabled) AppWhite else TextPrimary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (enabled)
                    androidx.compose.ui.graphics.Color(0xFF4CAF50)
                else
                    androidx.compose.ui.graphics.Color(0xFFE0E0E0)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (pulse) 0.2f else 1f)
            .padding(vertical = 10.dp)
            .fillMaxWidth(),
    )
}

@Composable
private fun PermissionItemRow(
    iconRes: Int,
    label: String,
    switchEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Color(0xFFF5F5F5))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = TextPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )

        Image(
            painter = painterResource(
                if (switchEnabled) R.drawable.ic_switch_on else R.drawable.ic_switch_off
            ),
            contentDescription = null,
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .then(if (switchEnabled) Modifier else Modifier.clickable(onClick = onToggle)),
        )
    }
}
