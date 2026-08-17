package com.smarttool.videodownloader.feature.downloads.domain.usecase

import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.feature.downloads.domain.model.VideoFormatOption

/**
 * Collapses the raw format list into one entry per distinct quality.
 *
 * Extractors return many near-duplicate formats (codec variants of the same
 * resolution); the picker only ever showed one chip per shortened label, so the
 * de-duplication is part of the contract rather than a display detail.
 */
class GetVideoFormatOptionsUseCase {

    operator fun invoke(videoInfo: VideoInfo): List<VideoFormatOption> {
        val byLabel = LinkedHashMap<String, VideoFormatOption>()

        for (format in videoInfo.formats.formats) {
            // Skip formats with no meaningful metadata to label or pick by (see
            // VideoFormatEntity.isUsable) — kept as a defensive filter here in case a
            // video's format list ever mixes a handful of these with otherwise-good
            // entries; the more important guard against a video made up *entirely* of
            // such entries lives in DetectedVideosTabViewModel.pushNewVideoInfoToAll,
            // since dropping every format here would leave an empty picker for that video.
            if (!format.isUsable) continue

            val label = shortLabel(format.format)
            if (label.isEmpty()) continue

            byLabel[label] = VideoFormatOption(
                label = label,
                format = format.format ?: continue,
                url = format.url,
                isAudio = label == AUDIO_LABEL || format.isAudioOnly,
            )
        }

        // Sorted low to high by resolution. toSortedMap() compared labels as plain
        // strings, which put "1080P" before "240P" (lexicographic, not numeric).
        return byLabel.values.sortedBy { qualityRank(it.label) }
    }

    /** Audio-only formats collapse to one [AUDIO_LABEL] chip instead of a resolution. */
    private fun shortLabel(format: String?): String {
        val readable = (format ?: return ERROR).replace(Regex("-\\w+"), "")

        return when {
            readable.contains("x") ->
                readable.substringAfterLast("x").replace(Regex("\\D"), "") + "P"

            readable.contains("audio only") -> AUDIO_LABEL

            readable.contains("-") -> {
                val left = readable.substringBefore("-")
                if (left.lowercase().contains("hd") || left.contains("sd")) {
                    left.trim().uppercase()
                } else {
                    readable.substringAfterLast("-").replace("p", "P").trim()
                }
            }

            else -> readable
        }
    }

    /**
     * Numeric labels (e.g. "720P") rank by their own number. "HD"/"SD" have no
     * resolution digits to sort by, so they rank at the conventional pixel height
     * that label implies — this interleaves them correctly next to numeric labels
     * instead of always landing first/last regardless of actual quality. Anything
     * else unrecognized (e.g. a watermarked "download" format with no known
     * width/height, or the "MP4"/"Error" fallbacks) sorts *before* every real
     * resolution — DetectedVideoUiMapper defaults the picker to `options.last()`
     * as "best quality", so ranking an unknown-resolution format anywhere near the
     * high end makes it win that default over formats we actually know are better.
     */
    private fun qualityRank(label: String): Int {
        if (label == AUDIO_LABEL) return Int.MAX_VALUE
        label.takeWhile { it.isDigit() }.toIntOrNull()?.let { return it }

        return when (label) {
            "SD" -> SD_RANK
            "HD" -> HD_RANK
            else -> Int.MIN_VALUE
        }
    }

    private companion object {
        const val ERROR = "Error"
        const val SD_RANK = 480
        const val HD_RANK = 720
        const val AUDIO_LABEL = "Audio"
    }
}
