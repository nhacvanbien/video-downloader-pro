package com.smarttool.videodownloader.feature.history.domain.usecase

import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry
import com.smarttool.videodownloader.feature.history.domain.repository.HistoryRepository

class DeleteHistoryEntryUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke(entry: HistoryEntry) = repository.delete(entry)
}
