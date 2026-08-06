package com.smarttool.videodownloader.feature.media.data

import com.smarttool.videodownloader.feature.media.domain.MediaPreferencesRepository
import com.smarttool.videodownloader.helper.PreferenceHelper

class MediaPreferencesRepositoryImpl(
    private val preferenceHelper: PreferenceHelper,
) : MediaPreferencesRepository {

    override fun playbackSpeed(): Float = preferenceHelper.getSpeedMedia()

    override fun setPlaybackSpeed(speed: Float) = preferenceHelper.setSpeedMedia(speed)

    override fun isLooping(): Boolean = preferenceHelper.getIsLoopMedia()

    override fun setLooping(looping: Boolean) = preferenceHelper.setIsLoopMedia(looping)

    override fun isFillMode(): Boolean = preferenceHelper.getIsFillMedia()

    override fun setFillMode(fill: Boolean) = preferenceHelper.setIsFillMedia(fill)
}
