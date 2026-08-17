package com.smarttool.videodownloader.feature.rating.data

import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.feature.rating.domain.repository.RatingPromptRepository
import kotlinx.coroutines.flow.first

class RatingPromptRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
) : RatingPromptRepository {
    override suspend fun isPromptShown(): Boolean = preferences.ratingPromptShown.first()

    override suspend fun markPromptShown() = preferences.setRatingPromptShown(true)

    override suspend fun incrementSuccessfulDownloadCount(): Int =
        preferences.incrementRatingSuccessfulDownloadCount()

    override suspend fun getLastPromptedDownloadCount(): Int =
        preferences.ratingLastPromptedDownloadCount.first()

    override suspend fun setLastPromptedDownloadCount(count: Int) =
        preferences.setRatingLastPromptedDownloadCount(count)
}
