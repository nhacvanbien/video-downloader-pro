package com.smarttool.videodownloader.core.ui.components

/** What a downloaded/detected file is, so its thumbnail can show a type-distinguishing icon. */
enum class MediaKind {
    VIDEO,
    AUDIO,
    IMAGE,
    ;

    companion object {
        fun fromMimeType(mimeType: String): MediaKind = when {
            mimeType.startsWith("image") -> IMAGE
            mimeType.startsWith("audio") -> AUDIO
            else -> VIDEO
        }
    }
}
