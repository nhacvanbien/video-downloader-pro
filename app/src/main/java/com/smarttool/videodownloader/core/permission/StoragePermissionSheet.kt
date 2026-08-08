package com.smarttool.videodownloader.core.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.smarttool.videodownloader.VideoDownloaderApplication
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.android.databinding.LayoutBottomSheetPermissionBinding

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

    private val pulse = AlphaAnimation(1f, 0.2f).apply {
        duration = PULSE_DURATION_MILLIS
        interpolator = LinearInterpolator()
        repeatCount = Animation.INFINITE
        repeatMode = Animation.REVERSE
    }

    private val dialog by lazy {
        BottomSheetDialog(activity, R.style.CustomAlertBottomSheet)
    }

    private var binding: LayoutBottomSheetPermissionBinding? = null

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
        val layout = LayoutBottomSheetPermissionBinding.inflate(activity.layoutInflater)
        binding = layout

        dialog.setContentView(layout.root)
        dialog.setCanceledOnTouchOutside(true)

        val behavior = dialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.addBottomSheetCallback(KeepExpandedCallback(behavior))

        layout.btnClose.setOnClickListener { dialog.dismiss() }
        layout.btnNotification.setOnClickListener {
            restartPulse()
            requestNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        layout.btnStorage.setOnClickListener {
            restartPulse()
            requestStorage()
        }

        refreshButtons()
        dialog.show()
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

    /**
     * Only one of the two buttons is ever actionable: storage first, notifications
     * once storage is granted. The sheet's icon and caption follow the active one.
     */
    private fun refreshButtons() {
        val layout = binding ?: return
        val storageDone = checker.hasStorage()

        restartPulse()

        layout.btnStorage.clearAnimation()
        layout.btnNotification.clearAnimation()

        layout.btnStorage.isEnabled = !storageDone
        layout.btnNotification.isEnabled = storageDone

        if (storageDone) {
            layout.tvDes.text = activity.getString(R.string.string_notification)
            layout.imgStorage.setImageResource(R.drawable.ic_notification)
        } else {
            layout.tvDes.text = activity.getString(R.string.string_storage)
            layout.imgStorage.setImageResource(R.drawable.ic_storage)
        }

        styleButton(layout.btnStorage, enabled = !storageDone)
        styleButton(layout.btnNotification, enabled = storageDone)
    }

    private fun styleButton(button: TextView, enabled: Boolean) {
        if (enabled) {
            button.setBackgroundResource(R.drawable.bg_btn_skip_permission)
            button.setTextColor(ENABLED_TEXT_COLOR.toColorInt())
        } else {
            button.setBackgroundResource(R.drawable.bg_btn_exit)
            button.setTextColor(DISABLED_TEXT_COLOR.toColorInt())
        }
    }

    /** Draws attention to whichever button is still actionable after a short idle. */
    private fun restartPulse() {
        pulseRunnable?.let { handler.removeCallbacks(it) }

        val runnable = Runnable {
            val layout = binding ?: return@Runnable
            when {
                layout.btnStorage.isEnabled -> layout.btnStorage.startAnimation(pulse)
                layout.btnNotification.isEnabled -> layout.btnNotification.startAnimation(pulse)
            }
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

        VideoDownloaderApplication.instance.appResumeAdHelper.setDisableAppResumeOnScreen()
        openAppSettings.launch(intent)
    }

    private companion object {
        const val PULSE_DELAY_MILLIS = 5000L
        const val PULSE_DURATION_MILLIS = 700L
        const val ENABLED_TEXT_COLOR = "#FFFFFF"
        const val DISABLED_TEXT_COLOR = "#808080"
    }
}
