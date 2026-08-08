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
import androidx.compose.ui.res.painterResource
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.AppGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

/**
 * Thumbnail for a downloaded file. Video frames are extracted off the main thread
 * (there is no Compose image loader in this project); images decode from the path.
 * Falls back to the play icon, as the View-based adapter did.
 */
@Composable
fun MediaThumbnail(
    filePath: String,
    isImage: Boolean,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(filePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(filePath, isImage) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (isImage) {
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
