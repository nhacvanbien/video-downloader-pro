package com.smarttool.videodownloader.feature.downloads.data

import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.feature.downloads.domain.WifiOnlyRepository
import kotlinx.coroutines.flow.Flow

class WifiOnlyRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
) : WifiOnlyRepository {

    override val wifiOnly: Flow<Boolean> = preferences.wifiOnly

    override suspend fun setWifiOnly(enabled: Boolean) = preferences.setWifiOnly(enabled)
}
