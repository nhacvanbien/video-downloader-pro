package com.smarttool.videodownloader.feature.browser.data

import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.feature.browser.domain.SearchEngineRepository
import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchEngineRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
) : SearchEngineRepository {

    override val searchEngine: Flow<SearchEngine> =
        preferences.searchEngineId.map { SearchEngine.fromId(it) }

    override suspend fun setSearchEngine(engine: SearchEngine) =
        preferences.setSearchEngineId(engine.id)
}
