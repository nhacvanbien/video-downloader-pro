package com.smarttool.videodownloader.core.ui.components

/** What a downloaded/detected file is, so its thumbnail can show a type-distinguishing icon. */
enum class MediaKind {
    VIDEO,
    AUDIO,
    IMAGE,
    ;

    companion object {
        val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus")
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

        fun fromMimeType(mimeType: String): MediaKind = when {
            mimeType.startsWith("image") -> IMAGE
            mimeType.startsWith("audio") -> AUDIO
            else -> VIDEO
        }

        fun fromExtension(extension: String): MediaKind = when (extension.lowercase()) {
            in AUDIO_EXTENSIONS -> AUDIO
            in IMAGE_EXTENSIONS -> IMAGE
            else -> VIDEO
        }

        /**
         * Resolves a stored row's kind. The persisted mime type only started distinguishing
         * audio from video recently, so rows written before that claim "video" for audio
         * files; the extension is the trustworthy signal and wins whenever it identifies a
         * non-video file.
         */
        fun forFile(mimeType: String, fileName: String): MediaKind {
            val byExtension = fromExtension(fileName.substringAfterLast('.', ""))
            return if (byExtension != VIDEO) byExtension else fromMimeType(mimeType)
        }
    }
}
