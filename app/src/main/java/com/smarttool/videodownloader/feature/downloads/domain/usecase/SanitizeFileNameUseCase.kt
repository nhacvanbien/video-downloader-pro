package com.smarttool.videodownloader.feature.downloads.domain.usecase

/**
 * Strips characters that are unsafe in a file name — punctuation outside a small
 * allow-list, plus emoji and unassigned code points — and collapses the resulting
 * whitespace.
 */
class SanitizeFileNameUseCase {

    operator fun invoke(name: String): String {
        val withoutSpecials = SPECIAL_CHARS.replace(name, " ")
        val withoutEmoji = EMOJI.replace(withoutSpecials, " ")
        return withoutEmoji.replace(Regex("\\s+"), " ").trim()
    }

    private companion object {
        val SPECIAL_CHARS = Regex("[^a-zA-Z0-9 _.,!?]")
        val EMOJI = Regex("[\\p{So}\\p{Cn}]")
    }
}
