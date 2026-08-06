package com.smarttool.videodownloader.feature.history.domain.repository

import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeHistory(query: String): Flow<List<HistoryEntry>>

    fun observeBookmarks(query: String): Flow<List<HistoryEntry>>

    suspend fun save(entry: HistoryEntry)

    suspend fun delete(entry: HistoryEntry)

    suspend fun clearHistory()

    suspend fun fetchFavicon(url: String): ByteArray?
}
