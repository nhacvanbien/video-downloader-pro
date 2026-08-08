package com.smarttool.videodownloader.feature.downloads.presentation

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.feature.browser.presentation.DetectedVideosContract

/**
 * The Processing tab body. The host — and with it the detection WebView — is owned by
 * the Activity, so switching to another tab and back keeps the probe alive; this
 * composable only renders the ViewModels' state and dispatches events.
 */
@Composable
fun ProcessingRoute(
    host: ProcessingWebViewHost,
    onOpenGuide: () -> Unit,
    onPreviewMedia: (url: String, title: String, headers: String) -> Unit,
) {
    val context = LocalContext.current
    val processingViewModel = host.processingViewModel
    val detector = host.detector

    LaunchedEffect(Unit) {
        host.onPreviewMedia = onPreviewMedia
    }

    val vmState by processingViewModel.uiState.collectAsStateWithLifecycle()
    val detectorState by detector.uiState.collectAsStateWithLifecycle()
    val downloads by processingViewModel.downloads.collectAsStateWithLifecycle()

    val state = ProcessingUiState(
        url = vmState.url,
        downloadButtonState = detectorState.downloadButtonState.toUiState(),
    )

    ProcessingScreen(
        state = state,
        downloads = downloads,
        detectionWebView = host.detectionWebView,
        onUrlChange = { processingViewModel.onEvent(ProcessingContract.Event.UrlChange(it)) },
        onPasteClick = { pasteFromClipboard(context, processingViewModel) },
        onDownloadClick = { detector.onEvent(DetectedVideosContract.Event.ShowVideoInfo) },
        onGuideClick = onOpenGuide,
        onPauseResume = { info ->
            val event = if (info.downloadStatus == VideoTaskState.PAUSE) {
                ProcessingContract.Event.Resume(info)
            } else {
                ProcessingContract.Event.Pause(info)
            }
            processingViewModel.onEvent(event)
        },
        onCancel = { info ->
            processingViewModel.onEvent(ProcessingContract.Event.Cancel(info, removeFile = true))
        },
    )

    DetectedVideosSheetHost(presenter = host.detected)
}

private fun pasteFromClipboard(context: Context, viewModel: ProcessingViewModel) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val hasText = clipboard.hasPrimaryClip() &&
        clipboard.primaryClipDescription
            ?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true

    if (!hasText) {
        Toast.makeText(context, context.getString(R.string.string_no_text_in_clipboard), Toast.LENGTH_SHORT).show()
        return
    }

    val copied = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()

    if (copied.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.string_clipboard_is_empty), Toast.LENGTH_SHORT).show()
        return
    }

    viewModel.onEvent(ProcessingContract.Event.UrlChange(copied))
}
