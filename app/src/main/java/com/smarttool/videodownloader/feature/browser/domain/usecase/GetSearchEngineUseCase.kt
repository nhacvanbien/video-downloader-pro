package com.smarttool.videodownloader.feature.browser.domain.usecase

import com.smarttool.videodownloader.feature.browser.domain.SearchEngineRepository
import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine
import kotlinx.coroutines.flow.first

class GetSearchEngineUseCase(private val repository: SearchEngineRepository) {
    suspend operator fun invoke(): SearchEngine = repository.searchEngine.first()
}
