package com.smarttool.videodownloader.feature.main.data

import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.feature.main.domain.AppUsageRepository

class AppUsageRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
) : AppUsageRepository {

    override suspend fun recordAppExit() = preferences.incrementExitCount()
}
