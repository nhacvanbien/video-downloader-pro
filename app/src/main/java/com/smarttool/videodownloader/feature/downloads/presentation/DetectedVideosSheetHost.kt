package com.smarttool.videodownloader.feature.downloads.presentation

import androidx.compose.runtime.Composable

/**
 * Renders [DetectedVideosSheet] over a [DetectedVideosPresenter] when that presenter has
 * something to show. Both WebView hosts mount this, so neither route has to know how the
 * sheet's callbacks are wired.
 *
 * [presenter] is null while its host has no session running — the routes compose either
 * side of one — and there is nothing to show then.
 */
@Composable
fun DetectedVideosSheetHost(presenter: DetectedVideosPresenter?) {
    if (presenter == null || !presenter.isSheetVisible) return

    DetectedVideosSheet(
        videos = presenter.videos,
        onSelectFormat = { video, option ->
            presenter.onSelectFormatById(video.id, option.format)
        },
        onRename = presenter::rename,
        onPreview = presenter::preview,
        onDownload = presenter::download,
        onDismiss = presenter::dismiss,
    )
}
