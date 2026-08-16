package com.smarttool.videodownloader.feature.settings.domain

import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine

interface SettingsRepository {
    fun downloadLocation(): String

    suspend fun isRated(): Boolean

    suspend fun wifiOnly(): Boolean

    suspend fun searchEngine(): SearchEngine

    suspend fun downloadLocationSubfolder(): String

    suspend fun setDownloadLocationSubfolder(name: String)
}
