package com.smarttool.videodownloader.feature.browser.domain.usecase

import com.smarttool.videodownloader.feature.browser.domain.SearchEngineRepository
import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine
import kotlinx.coroutines.flow.Flow

class ObserveSearchEngineUseCase(private val repository: SearchEngineRepository) {
    operator fun invoke(): Flow<SearchEngine> = repository.searchEngine
}
