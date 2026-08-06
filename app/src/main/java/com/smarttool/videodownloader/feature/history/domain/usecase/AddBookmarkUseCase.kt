package com.smarttool.videodownloader.feature.history.domain.usecase

import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry
import com.smarttool.videodownloader.feature.history.domain.repository.HistoryRepository
import java.util.UUID

class AddBookmarkUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke(name: String, url: String) {
        repository.save(
            HistoryEntry(
                id = UUID.randomUUID().toString(),
                title = name,
                url = url,
                datetime = System.currentTimeMillis(),
                isBookmark = true,
                favicon = repository.fetchFavicon(url),
            ),
        )
    }
}
