package com.smarttool.videodownloader.feature.history.domain.usecase

import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry
import com.smarttool.videodownloader.feature.history.domain.repository.HistoryRepository

/** Records a visited page, or promotes it to a bookmark. */
class SaveHistoryEntryUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke(entry: HistoryEntry) = repository.save(entry)
}
