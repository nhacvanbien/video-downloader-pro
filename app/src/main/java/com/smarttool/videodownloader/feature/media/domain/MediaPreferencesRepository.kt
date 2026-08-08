package com.smarttool.videodownloader.feature.media.domain

/** Playback preferences that persist across sessions. */
interface MediaPreferencesRepository {
    suspend fun playbackSpeed(): Float

    suspend fun setPlaybackSpeed(speed: Float)

    suspend fun isLooping(): Boolean

    suspend fun setLooping(looping: Boolean)

    /** Fill mode plays fullscreen in landscape and hides the ad slot. */
    suspend fun isFillMode(): Boolean

    suspend fun setFillMode(fill: Boolean)
}
