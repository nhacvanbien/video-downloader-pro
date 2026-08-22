package com.smarttool.videodownloader.feature.downloads.presentation

import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.core.ui.components.MediaKind
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.library.domain.model.MediaFilter

/** A single row in the merged Downloads list — either still downloading/failed, or done. */
sealed class DownloadListItem(open val id: String) {
    abstract val displayTitle: String
    abstract val mediaType: MediaFilter

    /** Site the file came from, for the row's "Source • Format • Size" line; null when unknown. */
    abstract val sourceLabel: String?

    /** Container tag ("MP4", "MP3") for the same line; null when it cannot be determined. */
    abstract val formatLabel: String?

    /** Bytes on disk once finished, or the expected total while still downloading. */
    abstract val sizeBytes: Long

    /** Playback length in milliseconds for the thumbnail's duration badge; 0 when unknown. */
    abstract val durationMs: Long

    data class Active(val progressInfo: ProgressInfo) : DownloadListItem(progressInfo.id) {
        val isFailed: Boolean
            get() = progressInfo.downloadStatus == VideoTaskState.ERROR ||
                progressInfo.downloadStatus == VideoTaskState.ENOSPC

        val isWaitingForWifi: Boolean
            get() = progressInfo.downloadStatus == VideoTaskState.WAITING_FOR_WIFI

        override val displayTitle: String get() = progressInfo.videoInfo.title

        // An active download still knows the page it was detected on, which names the source
        // more reliably than the media CDN a completed row is left with.
        override val sourceLabel: String?
            get() = downloadSourceLabel(progressInfo.videoInfo.originalUrl)
                ?: downloadSourceLabel(progressInfo.videoInfo.firstUrlToString)

        override val formatLabel: String? get() = downloadFormatLabel(progressInfo.videoInfo.ext)

        override val sizeBytes: Long get() = progressInfo.progressTotal

        // yt-dlp reports duration in whole seconds (see VideoService), unlike the millisecond
        // value MediaMetadataRetriever writes onto a finished row.
        override val durationMs: Long get() = progressInfo.videoInfo.duration * 1000
        // Same signal the in-progress row's thumbnail uses (see DownloadsScreen): an audio-only
        // pick is served with a video container extension often enough that `ext` alone would
        // park it under the Video chip while the row already shows a music icon.
        override val mediaType: MediaFilter
            get() = if (progressInfo.videoInfo.isAudioOnly) {
                MediaFilter.Audio
            } else {
                classifyByExtension(progressInfo.videoInfo.ext)
            }
    }

    data class Completed(val videoTaskItem: VideoTaskItem) : DownloadListItem(videoTaskItem.mId) {
        override val displayTitle: String
            get() = videoTaskItem.title.ifBlank { videoTaskItem.fileName }

        override val mediaType: MediaFilter
            get() = MediaFilter.forFile(videoTaskItem.mimeType, videoTaskItem.fileName)

        override val sourceLabel: String? get() = downloadSourceLabel(videoTaskItem.url)

        override val formatLabel: String? get() = downloadFormatLabel(videoTaskItem.fileName)

        override val sizeBytes: Long get() = videoTaskItem.fileSize

        override val durationMs: Long get() = videoTaskItem.fileDuration
    }
}

private fun classifyByExtension(ext: String): MediaFilter = MediaKind.fromExtension(ext).toFilter()

private fun MediaKind.toFilter(): MediaFilter = when (this) {
    MediaKind.AUDIO -> MediaFilter.Audio
    MediaKind.IMAGE -> MediaFilter.Image
    MediaKind.VIDEO -> MediaFilter.Video
}

/**
 * Filter for rows the Downloads tab holds itself (the active downloads); completed rows arrive
 * already filtered by [com.smarttool.videodownloader.feature.library.domain.MediaLibraryRepository],
 * which classifies through the same [MediaFilter.forFile].
 */
fun DownloadListItem.matches(filter: MediaFilter, search: String): Boolean {
    val typeMatches = filter == MediaFilter.All || mediaType == filter
    val searchMatches = search.isBlank() || displayTitle.contains(search, ignoreCase = true)
    return typeMatches && searchMatches
}
