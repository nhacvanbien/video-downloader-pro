package com.smarttool.videodownloader.feature.media.domain

/** Playback preferences that persist across sessions. */
interface MediaPreferencesRepository {
    fun playbackSpeed(): Float

    fun setPlaybackSpeed(speed: Float)

    fun isLooping(): Boolean

    fun setLooping(looping: Boolean)

    /** Fill mode plays fullscreen in landscape and hides the ad slot. */
    fun isFillMode(): Boolean

    fun setFillMode(fill: Boolean)
}
