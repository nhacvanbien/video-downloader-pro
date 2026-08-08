package com.smarttool.videodownloader.feature.media.data

import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.feature.media.domain.MediaPreferencesRepository
import kotlinx.coroutines.flow.first

class MediaPreferencesRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
) : MediaPreferencesRepository {

    override suspend fun playbackSpeed(): Float = preferences.playbackSpeed.first()

    override suspend fun setPlaybackSpeed(speed: Float) = preferences.setPlaybackSpeed(speed)

    override suspend fun isLooping(): Boolean = preferences.playbackLooping.first()

    override suspend fun setLooping(looping: Boolean) = preferences.setPlaybackLooping(looping)

    override suspend fun isFillMode(): Boolean = preferences.playbackFillMode.first()

    override suspend fun setFillMode(fill: Boolean) = preferences.setPlaybackFillMode(fill)
}
