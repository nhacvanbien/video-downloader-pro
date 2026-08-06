package com.smarttool.videodownloader.feature.history.domain.usecase

import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry
import com.smarttool.videodownloader.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow

class ObserveHistoryUseCase(private val repository: HistoryRepository) {
    operator fun invoke(query: String): Flow<List<HistoryEntry>> = repository.observeHistory(query)
}
