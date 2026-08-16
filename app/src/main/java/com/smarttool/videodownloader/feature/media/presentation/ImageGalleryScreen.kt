package com.smarttool.videodownloader.feature.media.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.AppBlack
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The gallery body: a [HorizontalPager] of [ZoomableImage]s, one per [ImageGalleryContract
 * .State.images] entry. Pager swipe is disabled while the current page is pinch-zoomed in —
 * otherwise a horizontal pan meant to inspect the zoomed image gets read as a page turn.
 */
@Composable
fun ImageGalleryScreen(
    state: ImageGalleryContract.State,
    pagerState: PagerState,
    onBack: () -> Unit,
    onMore: () -> Unit,
) {
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) { isCurrentPageZoomed = false }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val currentTitle = state.currentItem?.let { it.title.ifBlank { it.fileName } } ?: state.initialTitle

        ImageGalleryHeader(
            title = currentTitle,
            pageLabel = if (state.images.size > 1) {
                "${pagerState.currentPage + 1}/${state.images.size}"
            } else {
                null
            },
            showMoreAction = state.showMoreAction,
            onBack = onBack,
            onMore = onMore,
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isCurrentPageZoomed,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                ZoomableImage(
                    filePath = state.images.getOrNull(page)?.filePath.orEmpty(),
                    onZoomChanged = { zoomed -> if (page == pagerState.currentPage) isCurrentPageZoomed = zoomed },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** Shown while [ImageGalleryViewModel] is still resolving the sibling image list. */
@Composable
fun ImageGalleryLoadingScreen(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ImageGalleryHeader(
            title = title,
            pageLabel = null,
            showMoreAction = false,
            onBack = onBack,
            onMore = {},
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppWhite)
        }
    }
}

@Composable
private fun ImageGalleryHeader(
    title: String,
    pageLabel: String?,
    showMoreAction: Boolean,
    onBack: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = null,
            modifier = Modifier.size(28.dp).clickable(onClick = onBack).padding(4.dp),
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = AppWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )

            if (pageLabel != null) {
                Text(
                    text = pageLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppWhite.copy(alpha = 0.7f),
                )
            }
        }

        if (showMoreAction) {
            Image(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = null,
                modifier = Modifier.size(22.dp).clickable(onClick = onMore),
            )
        }
    }
}

/**
 * Pinch-to-zoom (1x–5x) with pan clamped to reset once the user zooms back out, plus a
 * double-tap shortcut between 1x and 3x. Decodes off the main thread at a sample size
 * large enough to stay sharp under zoom without risking OOM on a full-resolution photo —
 * mirrors `MediaThumbnail`'s downsampling since there is no Compose image loader in this
 * project (Glide is the only image dependency, and its Compose integration isn't pulled in).
 */
@Composable
private fun ZoomableImage(filePath: String, onZoomChanged: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(filePath) { mutableStateOf<Bitmap?>(null) }
    var scale by remember(filePath) { mutableFloatStateOf(1f) }
    var offset by remember(filePath) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(filePath) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching { decodeFullImage(context, filePath) }.getOrNull()
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val frame = bitmap

        if (frame != null) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    )
                    .pointerInput(filePath) {
                        // A plain detectTransformGestures() consumes every touch-move once
                        // it clears touch slop, single-finger swipes included — the parent
                        // HorizontalPager would never see the drag and could not turn the
                        // page. Only claim the gesture for a genuine pinch (2+ pointers) or
                        // for panning while already zoomed in; a one-finger drag at 1x is
                        // left untouched so it falls through to the pager as a page swipe.
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val isPinch = event.changes.size > 1

                                if (isPinch || scale > 1f) {
                                    val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                    scale = newScale
                                    offset = if (newScale <= 1f) Offset.Zero else offset + panChange
                                    onZoomChanged(newScale > 1f)
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .pointerInput(filePath) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 3f
                                }
                                onZoomChanged(scale > 1f)
                            },
                        )
                    },
            )
        } else {
            CircularProgressIndicator(color = AppWhite)
        }
    }
}

/** Headroom over the screen width so pinch-zoom still has real pixels to show. */
private const val ZOOM_HEADROOM = 2

private fun decodeFullImage(context: Context, filePath: String): Bitmap? {
    val targetWidth = context.resources.displayMetrics.widthPixels * ZOOM_HEADROOM

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(filePath, bounds)

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateSampleSize(bounds.outWidth, targetWidth)
    }

    return BitmapFactory.decodeFile(filePath, options)
}

private fun calculateSampleSize(sourceWidth: Int, targetWidth: Int): Int {
    var sampleSize = 1
    while (sourceWidth / sampleSize > targetWidth) sampleSize *= 2
    return sampleSize
}
