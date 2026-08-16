package com.smarttool.videodownloader.feature.media.presentation

import android.widget.Toast
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.file.IntentUtil
import com.smarttool.videodownloader.core.ui.dialogs.DialogConfirmDelete
import com.smarttool.videodownloader.core.ui.dialogs.DialogRename
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File

/**
 * Full-screen, swipeable image viewing. There is no cross-screen bridge in this app for
 * passing a whole list through Navigation-Compose (see [ImageGalleryViewModel]'s doc), so
 * only the tapped item's `url`/`title` travel through the route — [ImageGalleryViewModel]
 * re-derives every sibling image to swipe through once it loads.
 *
 * [rememberPagerState] fixes its `initialPage` at first composition, so the pager is only
 * created once loading finishes and the real start index is known — creating it earlier
 * would always open on page 0 instead of the tapped image.
 */
@Composable
fun ImageGalleryRoute(
    url: String,
    title: String,
    isDownloaded: Boolean,
    onBack: () -> Unit,
) {
    val viewModel: ImageGalleryViewModel = koinViewModel()
    val context = LocalContext.current
    val intentUtil: IntentUtil = koinInject()

    LaunchedEffect(url) {
        viewModel.onEvent(
            ImageGalleryContract.Event.Load(title = title, showMoreAction = isDownloaded, filePath = url),
        )
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) {
        ImageGalleryLoadingScreen(title = title, onBack = onBack)
        return
    }

    val pagerState = rememberPagerState(initialPage = state.startIndex) {
        state.images.size.coerceAtLeast(1)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page -> viewModel.onEvent(ImageGalleryContract.Event.PageChanged(page)) }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ImageGalleryContract.Effect.AllImagesDeleted -> onBack()
                ImageGalleryContract.Effect.RenameFailed -> Toast.makeText(
                    context,
                    context.getString(R.string.string_invalid_data),
                    Toast.LENGTH_SHORT,
                ).show()
                is ImageGalleryContract.Effect.ScrollToPage -> pagerState.scrollToPage(effect.index)
            }
        }
    }

    ImageGalleryScreen(
        state = state,
        pagerState = pagerState,
        onBack = onBack,
        onMore = { viewModel.onEvent(ImageGalleryContract.Event.ShowMoreMenu) },
    )

    if (state.moreMenuVisible) {
        state.currentItem?.let { item ->
            MediaMoreSheet(
                fileName = item.fileName,
                onRename = {
                    viewModel.onEvent(ImageGalleryContract.Event.HideMoreMenu)
                    DialogRename(context, item.fileName) { newName ->
                        viewModel.onEvent(ImageGalleryContract.Event.Rename(context, newName))
                    }.show()
                },
                onShare = {
                    viewModel.onEvent(ImageGalleryContract.Event.HideMoreMenu)
                    intentUtil.shareFile(context, File(item.filePath).toUri())
                },
                onDelete = {
                    viewModel.onEvent(ImageGalleryContract.Event.HideMoreMenu)
                    DialogConfirmDelete(context) {
                        viewModel.onEvent(ImageGalleryContract.Event.Delete)
                    }.show()
                },
                onDismiss = { viewModel.onEvent(ImageGalleryContract.Event.HideMoreMenu) },
            )
        }
    }
}
