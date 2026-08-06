package com.smarttool.videodownloader.feature.settings.data

import com.smarttool.videodownloader.feature.settings.domain.SettingsRepository
import com.smarttool.videodownloader.helper.PreferenceHelper
import com.smarttool.videodownloader.core.file.FileUtil

class SettingsRepositoryImpl(
    private val preferenceHelper: PreferenceHelper,
    private val fileUtil: FileUtil,
) : SettingsRepository {

    override fun downloadLocation(): String = fileUtil.folderDir.path

    override fun isRated(): Boolean = preferenceHelper.isRate()
}
