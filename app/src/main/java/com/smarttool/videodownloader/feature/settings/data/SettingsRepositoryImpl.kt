package com.smarttool.videodownloader.feature.settings.data

import android.content.Context
import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.core.file.FileNameCleaner
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.data.repository.VideoTaskItemRepository
import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine
import com.smarttool.videodownloader.feature.settings.domain.SettingsRepository
import com.smarttool.videodownloader.feature.settings.domain.model.AppInfo
import com.smarttool.videodownloader.feature.settings.domain.model.DownloadStats
import kotlinx.coroutines.flow.first

class SettingsRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
    private val fileUtil: FileUtil,
    private val videoTaskItemRepository: VideoTaskItemRepository,
    private val appContext: Context,
) : SettingsRepository {

    override fun downloadLocation(): String = fileUtil.folderDir.path

    override suspend fun isRated(): Boolean = preferences.ratingPromptShown.first()

    override suspend fun wifiOnly(): Boolean = preferences.wifiOnly.first()

    override suspend fun searchEngine(): SearchEngine =
        SearchEngine.fromId(preferences.searchEngineId.first())

    override suspend fun downloadLocationSubfolder(): String =
        preferences.downloadLocationSubfolder.first()

    override suspend fun setDownloadLocationSubfolder(name: String) =
        preferences.setDownloadLocationSubfolder(FileNameCleaner.cleanFileName(name))

    override suspend fun getDownloadStats(): DownloadStats = DownloadStats(
        videoCount = videoTaskItemRepository.getDownloadedCount(),
        usedBytes = videoTaskItemRepository.getDownloadedTotalSize(),
        freeBytes = FileUtil.getFreeDiskSpace(fileUtil.folderDir),
    )

    override fun appInfo(): AppInfo {
        val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        return AppInfo(
            versionName = packageInfo.versionName.orEmpty(),
            lastUpdateTimeMillis = packageInfo.lastUpdateTime,
        )
    }
}
