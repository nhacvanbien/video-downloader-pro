package com.smarttool.videodownloader.feature.browser.domain

import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine
import kotlinx.coroutines.flow.Flow

/** Which engine the address bar / Browser Home search box submits non-URL input to. */
interface SearchEngineRepository {
    val searchEngine: Flow<SearchEngine>

    suspend fun setSearchEngine(engine: SearchEngine)
}
