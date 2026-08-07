package com.smarttool.videodownloader.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * The storage/notification grants the downloader needs, resolved per API level.
 *
 * The same two checks were written out four times across the old Activity/Fragment
 * hosts; they live here so every caller agrees on what "granted" means.
 */
class MediaPermissionChecker(private val context: Context) {

    fun hasStorage(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(Manifest.permission.READ_MEDIA_VIDEO) &&
                isGranted(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            isGranted(Manifest.permission.READ_EXTERNAL_STORAGE) &&
                isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

    /** Pre-Tiramisu notifications need no runtime grant, so they count as satisfied. */
    fun hasNotification(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

    fun hasAll(): Boolean = hasStorage() && hasNotification()

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
