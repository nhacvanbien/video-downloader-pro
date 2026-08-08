package com.smarttool.videodownloader.feature.history.data

import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.data.dao.HistoryDao
import com.smarttool.videodownloader.feature.browser.domain.FaviconUtils
import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry
import com.smarttool.videodownloader.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val MAX_HISTORY_ITEMS = 400

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao,
    private val okHttpProxyClient: OkHttpProxyClient,
    private val ioDispatcher: CoroutineDispatcher,
) : HistoryRepository {

    override fun observeHistory(query: String): Flow<List<HistoryEntry>> =
        historyDao.observeHistoryByTextSearch(query.toLikePattern())
            .map { items -> items.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeBookmarks(query: String): Flow<List<HistoryEntry>> =
        historyDao.observeBookmarkByTextSearch(query.toLikePattern())
            .map { items -> items.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun save(entry: HistoryEntry) = withContext(ioDispatcher) {
        historyDao.insertHistoryItem(entry.toEntity())

        val itemCount = historyDao.getItemCount()
        if (itemCount > MAX_HISTORY_ITEMS) {
            historyDao.deleteOldItems(itemCount - MAX_HISTORY_ITEMS)
        }
    }

    override suspend fun delete(entry: HistoryEntry) = withContext(ioDispatcher) {
        historyDao.deleteHistoryItem(entry.toEntity())
    }

    override suspend fun clearHistory() = withContext(ioDispatcher) {
        historyDao.clear()
    }

    override suspend fun fetchFavicon(url: String): ByteArray? = withContext(ioDispatcher) {
        try {
            val bitmap = FaviconUtils.getEncodedFaviconFromUrl(
                okHttpProxyClient.getProxyOkHttpClient(),
                url,
            )
            FaviconUtils.bitmapToBytes(bitmap)
        } catch (e: Throwable) {
            null
        }
    }

    private fun String.toLikePattern() = "%${trim()}%"
}
