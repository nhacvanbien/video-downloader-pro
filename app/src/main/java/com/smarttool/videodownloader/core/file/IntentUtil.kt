package com.smarttool.videodownloader.core.file

//import com.allVideoDownloaderXmaster.OpenForTesting

import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.smarttool.videodownloader.android.R
import java.io.File

//@OpenForTesting
class IntentUtil  constructor(private val fileUtil: FileUtil) {

    fun shareVideo(context: Context, uri: Uri) {

        val file = if (uri.scheme == null || uri.scheme == "file") {
            File(uri.path ?: "")
        } else {
            throw IllegalArgumentException("Unsupported URI scheme: ${uri.scheme}")
        }

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "video/*"
        val fileSupported = fileUtil.isFileApiSupportedByUri(context, uri)
        val shareUri = if (fileSupported) {
            FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                file
            )
        } else {
            // Q+ blocks handing out a raw file:// Uri for anything outside the app's own
            // storage (FileUriExposedException) — public Downloads entries only have a
            // content:// identity, resolved here from the MediaStore row this app wrote.
            resolvePublicDownloadUri(context, file) ?: run {
                Toast.makeText(
                    context,
                    context.getString(R.string.video_share_message),
                    Toast.LENGTH_SHORT,
                ).show()
                return
            }
        }

        intent.setDataAndType(shareUri, "video/mp4")
        intent.clipData = ClipData.newRawUri("", shareUri)
        intent.putExtra(Intent.EXTRA_STREAM, shareUri)

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val resInfoList: List<ResolveInfo> = context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(
                packageName,
                shareUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        if (intent.resolveActivityInfo(context.packageManager, 0) != null) {
            context.startActivity(Intent.createChooser(intent, "Share via:"))
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.video_share_message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** Looks up the content:// identity of a file this app previously wrote into the
     *  public Downloads collection — the only Uri form Q+ allows sharing outside the app. */
    private fun resolvePublicDownloadUri(context: Context, file: File): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(file.name)

        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
            }
        }

        return null
    }
}