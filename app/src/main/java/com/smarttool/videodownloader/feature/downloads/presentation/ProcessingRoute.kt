package com.smarttool.videodownloader.feature.downloads.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The Processing tab body. The controller — and with it the detection WebView — is
 * owned and started by the Activity, so switching to another tab and back keeps the
 * probe alive; this composable only renders it and wires up navigation.
 */
@Composable
fun ProcessingRoute(
    controller: ProcessingController,
    onOpenGuide: () -> Unit,
    onPreviewMedia: (url: String, title: String, headers: String) -> Unit,
) {
    LaunchedEffect(Unit) {
        controller.onPreviewMedia = onPreviewMedia
    }

    val downloads by controller.processingViewModel.downloads.collectAsStateWithLifecycle()

    ProcessingScreen(
        state = controller.uiState,
        downloads = downloads,
        detectionWebView = controller.detectionWebView,
        onUrlChange = controller::onUrlChange,
        onPasteClick = controller::pasteFromClipboard,
        onDownloadClick = controller::requestDetectedVideos,
        onGuideClick = onOpenGuide,
        onPauseResume = controller::onPauseResume,
        onCancel = controller::cancel,
    )

    if (controller.showDetectedSheet) {
        DetectedVideosSheet(
            videos = controller.detectedVideos,
            onSelectFormat = { video, option ->
                controller.onSelectFormatById(video.id, option.format)
            },
            onRename = controller::renameDetectedVideo,
            onPreview = controller::previewDetectedVideo,
            onDownload = controller::downloadDetectedVideo,
            onDismiss = { controller.showDetectedSheet = false },
        )
    }
}
