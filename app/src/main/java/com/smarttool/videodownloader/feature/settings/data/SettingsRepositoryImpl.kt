package com.smarttool.videodownloader.feature.settings.data

import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.first

class SettingsRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
    private val fileUtil: FileUtil,
) : SettingsRepository {

    override fun downloadLocation(): String = fileUtil.folderDir.path

    override suspend fun isRated(): Boolean = preferences.rated.first()
}
