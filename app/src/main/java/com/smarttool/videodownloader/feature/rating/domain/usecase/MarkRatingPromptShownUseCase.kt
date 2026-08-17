package com.smarttool.videodownloader.feature.rating.domain.usecase

import com.smarttool.videodownloader.feature.rating.domain.repository.RatingPromptRepository

class MarkRatingPromptShownUseCase(private val repository: RatingPromptRepository) {
    suspend operator fun invoke() = repository.markPromptShown()
}
