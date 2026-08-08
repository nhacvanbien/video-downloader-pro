package com.smarttool.videodownloader.feature.settings.domain

interface SettingsRepository {
    fun downloadLocation(): String

    suspend fun isRated(): Boolean
}
