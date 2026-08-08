package com.smarttool.videodownloader.core.ui.components

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.bumptech.glide.Glide
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.AppGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

/**
 * Thumbnail for a downloaded file, or for a detected-but-not-yet-downloaded video's
 * remote poster URL. Video frames and local images decode off the main thread (there
 * is no Compose image loader in this project); remote URLs go through Glide, which is
 * already a dependency but otherwise unused now that the View adapters are gone.
 * Falls back to the play icon, as the View-based adapter did.
 */
@Composable
fun MediaThumbnail(
    filePath: String,
    isImage: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember(filePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(filePath, isImage) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (filePath.startsWith("http")) {
                    Glide.with(context)
                        .asBitmap()
                        .load(filePath)
                        .submit(TARGET_WIDTH_PX, TARGET_WIDTH_PX)
                        .get()
                } else if (isImage) {
                    decodeScaledImage(filePath)
                } else {
                    filePath.toUri().path?.let {
                        ThumbnailUtils.createVideoThumbnail(
                            it,
                            MediaStore.Images.Thumbnails.MINI_KIND,
                        )
                    }
                }
            }.getOrNull()
        }
    }

    Box(modifier = modifier.background(AppGray), contentAlignment = Alignment.Center) {
        val frame = bitmap

        if (frame != null) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_play_download),
                contentDescription = null,
            )
        }
    }
}

/**
 * Decodes at a reduced sample size — full-resolution downloads are large enough to
 * stall the list if decoded at native size.
 */
private fun decodeScaledImage(filePath: String): Bitmap? {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(filePath, bounds)

    val options = android.graphics.BitmapFactory.Options().apply {
        inSampleSize = calculateSampleSize(bounds.outWidth, TARGET_WIDTH_PX)
    }

    return android.graphics.BitmapFactory.decodeFile(filePath, options)
}

private const val TARGET_WIDTH_PX = 256

private fun calculateSampleSize(sourceWidth: Int, targetWidth: Int): Int {
    var sampleSize = 1
    while (sourceWidth / sampleSize > targetWidth) {
        sampleSize *= 2
    }
    return sampleSize
}
