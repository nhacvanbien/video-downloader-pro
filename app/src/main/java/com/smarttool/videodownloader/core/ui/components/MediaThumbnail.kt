package com.smarttool.videodownloader.core.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import java.io.File
import android.util.Size

/**
 * Thumbnail for a downloaded file, or for a detected-but-not-yet-downloaded video's
 * remote poster URL. Video frames and local images decode off the main thread (there
 * is no Compose image loader in this project); remote URLs go through Glide, which is
 * already a dependency but otherwise unused now that the View adapters are gone.
 *
 * Always shows a small centered [mediaType] badge so video/audio/image rows stay
 * distinguishable at a glance even when their real frames look alike (or, for audio,
 * don't exist at all) — falls back to a full-size tinted glyph when no frame decodes.
 */
@Composable
fun MediaThumbnail(
    filePath: String,
    mediaType: MediaKind,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember(filePath) { mutableStateOf(MediaThumbnailCache.get(filePath)) }

    LaunchedEffect(filePath, mediaType) {
        if (bitmap != null) return@LaunchedEffect

        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                if (filePath.startsWith("http")) {
                    Glide.with(context)
                        .asBitmap()
                        .load(filePath)
                        .submit(TARGET_WIDTH_PX, TARGET_WIDTH_PX)
                        .get()
                } else {
                    when (mediaType) {
                        MediaKind.IMAGE -> decodeScaledImage(filePath)
                        MediaKind.VIDEO -> createVideoThumbnail(filePath)
                        // No frame to extract for a plain audio file.
                        MediaKind.AUDIO -> null
                    }
                }
            }.getOrNull()
        }

        if (decoded != null) {
            MediaThumbnailCache.put(filePath, decoded)
        }
        bitmap = decoded
    }

    Box(modifier = modifier.background(PriSoft), contentAlignment = Alignment.Center) {
        val frame = bitmap

        if (frame != null) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        when {
            frame == null -> Image(
                painter = painterResource(mediaType.fallbackIconRes()),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Pri),
            )

            mediaType == MediaKind.VIDEO -> Image(
                painter = painterResource(R.drawable.ic_play_media_fill),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )

            mediaType == MediaKind.IMAGE -> Box(
                modifier = Modifier.size(22.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_media_type_image),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.padding(4.dp),
                )
            }

            // Audio never has a real frame (see above), so this branch is unreached —
            // kept only so the `when` stays exhaustive over MediaKind.
            else -> Unit
        }
    }
}

private fun MediaKind.fallbackIconRes(): Int = when (this) {
    MediaKind.VIDEO -> R.drawable.ic_play_download
    MediaKind.AUDIO -> R.drawable.ic_music_note
    MediaKind.IMAGE -> R.drawable.ic_media_type_image
}

@SuppressLint("NewApi")
private fun createVideoThumbnail(filePath: String): Bitmap? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
        ThumbnailUtils.createVideoThumbnail(
            File(filePath),
            Size(TARGET_WIDTH_PX, TARGET_WIDTH_PX),
            null,
        )
    }
    else -> null
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
