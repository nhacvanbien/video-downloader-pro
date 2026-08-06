package com.smarttool.videodownloader.feature.history.domain.usecase

import com.smarttool.videodownloader.feature.history.domain.repository.HistoryRepository

class ClearHistoryUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke() = repository.clearHistory()
}
