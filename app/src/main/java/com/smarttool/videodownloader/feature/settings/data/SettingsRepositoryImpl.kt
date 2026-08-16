package com.smarttool.videodownloader.feature.settings.data

import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.core.file.FileNameCleaner
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine
import com.smarttool.videodownloader.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.first

class SettingsRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
    private val fileUtil: FileUtil,
) : SettingsRepository {

    override fun downloadLocation(): String = fileUtil.folderDir.path

    override suspend fun isRated(): Boolean = preferences.rated.first()

    override suspend fun wifiOnly(): Boolean = preferences.wifiOnly.first()

    override suspend fun searchEngine(): SearchEngine =
        SearchEngine.fromId(preferences.searchEngineId.first())

    override suspend fun downloadLocationSubfolder(): String =
        preferences.downloadLocationSubfolder.first()

    override suspend fun setDownloadLocationSubfolder(name: String) =
        preferences.setDownloadLocationSubfolder(FileNameCleaner.cleanFileName(name))
}
