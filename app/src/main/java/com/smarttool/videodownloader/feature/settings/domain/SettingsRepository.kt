package com.smarttool.videodownloader.feature.settings.domain

import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine
import com.smarttool.videodownloader.feature.settings.domain.model.AppInfo
import com.smarttool.videodownloader.feature.settings.domain.model.DownloadStats

interface SettingsRepository {
    fun downloadLocation(): String

    suspend fun isRated(): Boolean

    suspend fun wifiOnly(): Boolean

    suspend fun searchEngine(): SearchEngine

    suspend fun downloadLocationSubfolder(): String

    suspend fun setDownloadLocationSubfolder(name: String)

    suspend fun getDownloadStats(): DownloadStats

    fun appInfo(): AppInfo
}
