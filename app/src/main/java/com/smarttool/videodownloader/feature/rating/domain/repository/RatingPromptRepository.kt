package com.smarttool.videodownloader.feature.rating.domain.repository

interface RatingPromptRepository {
    suspend fun isPromptShown(): Boolean

    suspend fun markPromptShown()

    suspend fun incrementSuccessfulDownloadCount(): Int

    suspend fun getLastPromptedDownloadCount(): Int

    suspend fun setLastPromptedDownloadCount(count: Int)
}
