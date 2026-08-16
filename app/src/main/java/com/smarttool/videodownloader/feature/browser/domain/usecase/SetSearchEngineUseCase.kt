package com.smarttool.videodownloader.feature.browser.domain.usecase

import com.smarttool.videodownloader.feature.browser.domain.SearchEngineRepository
import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine

class SetSearchEngineUseCase(private val repository: SearchEngineRepository) {
    suspend operator fun invoke(engine: SearchEngine) = repository.setSearchEngine(engine)
}
