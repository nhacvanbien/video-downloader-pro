package com.smarttool.videodownloader.feature.downloads.presentation

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.file.FileNameCleaner
import com.smarttool.videodownloader.core.ui.dialogs.DialogRename
import com.smarttool.videodownloader.data.network.entity.VideFormatEntityList
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.feature.browser.presentation.DetectedVideosTabViewModel
import com.smarttool.videodownloader.feature.downloads.domain.model.DownloadTabListener
import com.smarttool.videodownloader.feature.downloads.domain.usecase.SanitizeFileNameUseCase
import org.json.JSONObject

/**
 * The detected-videos bottom sheet: turns what the detector found into sheet rows, and
 * turns taps on those rows into a rename, a preview or a download.
 *
 * Both WebView hosts show the same sheet over their own detector, so each owns one of
 * these instead of repeating the wiring. The sheet's visibility and contents are Compose
 * state, read straight from the routes.
 */
class DetectedVideosPresenter(
    private val activity: AppCompatActivity,
    private val detector: DetectedVideosTabViewModel,
    private val processingViewModel: ProcessingViewModel,
    private val mapper: DetectedVideoUiMapper,
    private val sanitizeFileName: SanitizeFileNameUseCase,
    /**
     * Processing confirms a start with its own toast; the browser stays quiet here and
     * reports the finished download instead.
     */
    private val announceDownloadStart: Boolean,
    private val onPreviewMedia: (url: String, title: String, headers: String) -> Unit,
) : DownloadTabListener {

    var videos by mutableStateOf<List<DetectedVideoUi>>(emptyList())
        private set

    var isSheetVisible by mutableStateOf(false)
        private set

    /** Opens the sheet on whatever has been detected so far. */
    fun show() {
        refresh()
        isSheetVisible = true
    }

    fun dismiss() {
        isSheetVisible = false
    }

    fun onSelectFormatById(id: String, format: String) {
        val info = findVideoInfo(id) ?: return
        onSelectFormat(info, format)
        refresh()
    }

    fun rename(video: DetectedVideoUi) {
        DialogRename(activity, video.title) { newName ->
            val titles = detector.formatsTitles.get()?.toMutableMap() ?: mutableMapOf()
            titles[video.id] = newName
            detector.formatsTitles.set(titles)
            refresh()
        }.show()
    }

    fun preview(video: DetectedVideoUi) {
        val info = findVideoInfo(video.id) ?: return
        val format = video.selectedFormat ?: return
        onPreviewVideo(info, format, false)
    }

    fun download(video: DetectedVideoUi) {
        val info = findVideoInfo(video.id) ?: return
        val format = video.selectedFormat

        if (format == null) {
            toast(R.string.string_invalid_data)
            return
        }

        onDownloadVideo(info, format, sanitizeFileName(video.title))
        isSheetVisible = false
        toast(R.string.string_downloading)
    }

    override fun onCancel() {
        isSheetVisible = false
    }

    override fun onPreviewVideo(videoInfo: VideoInfo, format: String, isForce: Boolean) {
        val title = detector.formatsTitles.get()?.get(videoInfo.id).orEmpty()
        val currFormat = videoInfo.formats.formats.filter {
            it.format?.contains(format) ?: false
        }

        val first = currFormat.firstOrNull() ?: return

        val headers = first.httpHeaders
            ?.let { JSONObject(it as Map<*, *>).toString() }
            ?: "{}"

        onPreviewMedia(first.url.orEmpty(), title, if (isForce) "{}" else headers)
    }

    override fun onDownloadVideo(videoInfo: VideoInfo, format: String, videoTitle: String) {
        val info = videoInfo.copy(
            title = FileNameCleaner.cleanFileName(videoTitle),
            formats = VideFormatEntityList(
                videoInfo.formats.formats.filter { it.format?.contains(format) ?: false },
            ),
        )

        processingViewModel.start(info)
        isSheetVisible = false

        if (announceDownloadStart) toast(R.string.download_started)
    }

    override fun onSelectFormat(videoInfo: VideoInfo, format: String) {
        val formats = detector.selectedFormats.get()?.toMutableMap() ?: mutableMapOf()
        formats[videoInfo.id] = format
        detector.selectedFormats.set(formats)
    }

    /** Rebuilds the sheet model from the detector's current list and the user's edits. */
    private fun refresh() {
        val titles = detector.formatsTitles.get().orEmpty()
        val formats = detector.selectedFormats.get().orEmpty()

        videos = detector.detectedVideosList.get()
            .orEmpty()
            .reversed()
            .map { mapper(it, titles, formats) }
    }

    private fun findVideoInfo(id: String): VideoInfo? =
        detector.detectedVideosList.get()?.firstOrNull { it.id == id }

    private fun toast(messageRes: Int) {
        Toast.makeText(activity, activity.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}
