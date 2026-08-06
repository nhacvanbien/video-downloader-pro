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
            val label = shortLabel(format.format)
            if (label.isEmpty()) continue

            byLabel[label] = VideoFormatOption(
                label = label,
                format = format.format ?: continue,
                url = format.url,
            )
        }

        return byLabel.toSortedMap().values.toList()
    }

    /** Audio-only formats collapse to an empty label and are dropped by [invoke]. */
    private fun shortLabel(format: String?): String {
        val readable = (format ?: return ERROR).replace(Regex("-\\w+"), "")

        return when {
            readable.contains("x") ->
                readable.substringAfterLast("x").replace(Regex("\\D"), "") + "P"

            readable.contains("audio only") -> ""

            readable.contains("-") -> {
                val left = readable.substringBefore("-")
                if (left.lowercase().contains("hd") || left.contains("sd")) {
                    left.trim()
                } else {
                    readable.substringAfterLast("-").replace("p", "P").trim()
                }
            }

            else -> readable
        }
    }

    private companion object {
        const val ERROR = "Error"
    }
}
