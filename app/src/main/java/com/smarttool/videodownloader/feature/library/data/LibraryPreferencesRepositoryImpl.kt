package com.smarttool.videodownloader.feature.library.data

import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.feature.library.domain.LibraryPreferencesRepository
import kotlinx.coroutines.flow.first

class LibraryPreferencesRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
) : LibraryPreferencesRepository {

    override suspend fun sortType(): Int = preferences.sortType.first()

    override suspend fun setSortType(type: Int) = preferences.setSortType(type)
}
