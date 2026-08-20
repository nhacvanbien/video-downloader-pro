package com.smarttool.videodownloader.feature.library.domain.model

import com.smarttool.videodownloader.core.ui.components.MediaKind

/** Which slice of the downloaded library a query covers. */
enum class MediaFilter(val typeValue: String) {
    All("all"),
    Video("video"),
    Audio("audio"),
    Image("image"),
    ;

    companion object {
        /**
         * The single classifier every list and every chip filter must go through, so a row can
         * never show a music icon while still living under the Video chip. Delegates to
         * [MediaKind.forFile] — the stored `mime_type` alone is not trustworthy for older rows.
         */
        fun forFile(mimeType: String, fileName: String): MediaFilter =
            when (MediaKind.forFile(mimeType, fileName)) {
                MediaKind.AUDIO -> Audio
                MediaKind.IMAGE -> Image
                MediaKind.VIDEO -> Video
            }
    }
}
