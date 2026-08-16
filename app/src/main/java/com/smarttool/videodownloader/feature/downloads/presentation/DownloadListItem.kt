package com.smarttool.videodownloader.feature.downloads.presentation

import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.library.domain.model.MediaFilter

private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus")
private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

/** A single row in the merged Downloads list — either still downloading/failed, or done. */
sealed class DownloadListItem(open val id: String) {
    abstract val displayTitle: String
    abstract val mediaType: MediaFilter

    data class Active(val progressInfo: ProgressInfo) : DownloadListItem(progressInfo.id) {
        val isFailed: Boolean
            get() = progressInfo.downloadStatus == VideoTaskState.ERROR ||
                progressInfo.downloadStatus == VideoTaskState.ENOSPC

        val isWaitingForWifi: Boolean
            get() = progressInfo.downloadStatus == VideoTaskState.WAITING_FOR_WIFI

        override val displayTitle: String get() = progressInfo.videoInfo.title
        override val mediaType: MediaFilter get() = classifyByExtension(progressInfo.videoInfo.ext)
    }

    data class Completed(val videoTaskItem: VideoTaskItem) : DownloadListItem(videoTaskItem.mId) {
        override val displayTitle: String
            get() = videoTaskItem.title.ifBlank { videoTaskItem.fileName }

        override val mediaType: MediaFilter
            get() = when {
                videoTaskItem.mimeType.startsWith("image") -> MediaFilter.Image
                videoTaskItem.mimeType.startsWith("audio") -> MediaFilter.Audio
                else -> MediaFilter.Video
            }
    }
}

private fun classifyByExtension(ext: String): MediaFilter = when (ext.lowercase()) {
    in AUDIO_EXTENSIONS -> MediaFilter.Audio
    in IMAGE_EXTENSIONS -> MediaFilter.Image
    else -> MediaFilter.Video
}

/** Client-side filter mirroring the SQL predicate [MediaLibraryRepository] applies to completed items. */
fun DownloadListItem.matches(filter: MediaFilter, search: String): Boolean {
    val typeMatches = filter == MediaFilter.All || mediaType == filter
    val searchMatches = search.isBlank() || displayTitle.contains(search, ignoreCase = true)
    return typeMatches && searchMatches
}
