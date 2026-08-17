package com.smarttool.videodownloader.feature.rating.presentation

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Pri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * There is no Compose image loader in this project (see [com.smarttool.videodownloader
 * .core.ui.components.MediaThumbnail]) — Glide decodes off the main thread into a plain
 * [Bitmap], same as that component.
 */
@Composable
fun FeedbackMediaThumbnail(uri: Uri, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                Glide.with(context).asBitmap().load(uri).submit(TARGET_SIZE_PX, TARGET_SIZE_PX).get()
            }.getOrNull()
        }
    }

    Box(modifier = modifier.size(48.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Border),
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_close_circle_fill),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(16.dp)
                .clip(RoundedCornerShape(50))
                .background(Pri)
                .padding(2.dp)
                .clickable(onClick = onRemove),
        )
    }
}

private const val TARGET_SIZE_PX = 128
