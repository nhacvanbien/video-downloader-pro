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
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.smarttool.videodownloader.android.R
import java.io.File

//@OpenForTesting
class IntentUtil  constructor(private val fileUtil: FileUtil) {

    fun shareFile(context: Context, uri: Uri) {

        val file = if (uri.scheme == null || uri.scheme == "file") {
            File(uri.path ?: "")
        } else {
            throw IllegalArgumentException("Unsupported URI scheme: ${uri.scheme}")
        }

        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = mimeType
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
            resolvePublicMediaUri(context, file, mimeType) ?: run {
                Toast.makeText(
                    context,
                    context.getString(R.string.video_share_message),
                    Toast.LENGTH_SHORT,
                ).show()
                return
            }
        }

        intent.setDataAndType(shareUri, mimeType)
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

    /** Looks up the content:// identity of a file this app previously wrote into a public
     *  MediaStore collection — the only Uri form Q+ allows sharing outside the app. Images are
     *  inserted into `MediaStore.Images` (Pictures/) while videos land in `MediaStore.Downloads`
     *  (Download/), so the matching collection is tried first, with Downloads as a fallback. */
    private fun resolvePublicMediaUri(context: Context, file: File, mimeType: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val candidateCollections = when {
            mimeType.startsWith("image/") -> listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            mimeType.startsWith("video/") -> listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            mimeType.startsWith("audio/") -> listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            else -> emptyList()
        } + MediaStore.Downloads.EXTERNAL_CONTENT_URI

        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(file.name)

        for (collection in candidateCollections) {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    return ContentUris.withAppendedId(collection, id)
                }
            }
        }

        return null
    }
}