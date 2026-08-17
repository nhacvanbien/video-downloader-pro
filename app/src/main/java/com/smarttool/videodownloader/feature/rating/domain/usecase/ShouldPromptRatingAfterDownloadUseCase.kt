package com.smarttool.videodownloader.feature.rating.domain.usecase

import com.smarttool.videodownloader.feature.rating.domain.repository.RatingPromptRepository

/**
 * Prompts after the user has felt the app deliver value, not on every launch. First ask
 * comes on the [FIRST_PROMPT_DOWNLOAD_COUNT]th successful download; if the user dismisses
 * it without rating, it waits another [PROMPT_COOLDOWN_DOWNLOADS] successful downloads
 * before asking again. Once a rating or feedback is actually submitted,
 * [RatingPromptRepository.isPromptShown] is set and this never returns true again.
 */
class ShouldPromptRatingAfterDownloadUseCase(private val repository: RatingPromptRepository) {
    suspend operator fun invoke(): Boolean {
        if (repository.isPromptShown()) return false

        val downloadCount = repository.incrementSuccessfulDownloadCount()
        val lastPrompted = repository.getLastPromptedDownloadCount()
        val nextPromptAt = if (lastPrompted == 0) {
            FIRST_PROMPT_DOWNLOAD_COUNT
        } else {
            lastPrompted + PROMPT_COOLDOWN_DOWNLOADS
        }
        if (downloadCount < nextPromptAt) return false

        repository.setLastPromptedDownloadCount(downloadCount)
        return true
    }

    private companion object {
        const val FIRST_PROMPT_DOWNLOAD_COUNT = 3
        const val PROMPT_COOLDOWN_DOWNLOADS = 10
    }
}
