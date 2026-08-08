package com.smarttool.videodownloader.feature.browser.data

import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.feature.browser.domain.BrowserPreferencesRepository
import kotlinx.coroutines.flow.first

class BrowserPreferencesRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
) : BrowserPreferencesRepository {

    override suspend fun isShowVideoAlert(): Boolean = preferences.isShowVideoAlert.first()

    override suspend fun setShowVideoAlert(show: Boolean) = preferences.setShowVideoAlert(show)

    override suspend fun isLockPortrait(): Boolean = preferences.isLockPortrait.first()

    override suspend fun videoDetectionThreshold(): Int =
        preferences.videoDetectionThreshold.first()
}
