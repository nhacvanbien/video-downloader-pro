package com.smarttool.videodownloader.feature.downloads.presentation

import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.feature.downloads.domain.usecase.GetVideoFormatOptionsUseCase

/**
 * Builds the sheet model for a detected video. [titles] and [selectedFormats] carry
 * the user's in-progress edits, which live in the detection ViewModel rather than on
 * the [VideoInfo] itself.
 */
class DetectedVideoUiMapper(
    private val getFormatOptions: GetVideoFormatOptionsUseCase,
) {

    operator fun invoke(
        videoInfo: VideoInfo,
        titles: Map<String, String?>,
        selectedFormats: Map<String, String?>,
    ): DetectedVideoUi {
        val options = getFormatOptions(videoInfo)

        return DetectedVideoUi(
            id = videoInfo.id,
            title = titles[videoInfo.id] ?: videoInfo.title,
            thumbnailUrl = videoInfo.thumbnail,
            typeLabel = typeLabel(videoInfo),
            readableSize = readableSize(videoInfo),
            formats = options,
            // The Audio chip always sorts last (see GetVideoFormatOptionsUseCase.qualityRank)
            // regardless of what video qualities are also on offer, since almost every
            // video carries an audio-only option alongside its real resolutions (see
            // VideoServiceLocal.handleYoutubeDlUrl) — so plain options.lastOrNull() picked
            // Audio as the default nearly every time a video existed to pick instead.
            // Preferring the best non-audio option keeps Audio as the fallback only for
            // genuinely audio-only results.
            selectedFormat = selectedFormats[videoInfo.id]
                ?: options.lastOrNull { !it.isAudio }?.format
                ?: options.lastOrNull()?.format,
        )
    }

    private fun typeLabel(videoInfo: VideoInfo): String {
        val isMpd = videoInfo.formats.formats.firstOrNull()?.url?.contains(".mpd") == true

        return when {
            videoInfo.isM3u8 -> if (isMpd) "MPD List" else "M3U8 List"
            videoInfo.isMaster -> if (isMpd) "MPD Master List" else "M3U8 Master List"
            videoInfo.isRegularDownload -> "Regular MP4 Download"
            else -> ""
        }
    }

    /** Only regular downloads report a size before the download starts. */
    private fun readableSize(videoInfo: VideoInfo): String? {
        if (!videoInfo.isRegularDownload) return null

        val bytes = videoInfo.formats.formats.firstOrNull()?.fileSize ?: return null
        return FileUtil.getFileSizeReadable(bytes.toDouble())
    }
}
