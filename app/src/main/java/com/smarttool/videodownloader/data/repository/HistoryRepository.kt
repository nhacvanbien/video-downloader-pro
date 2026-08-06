package com.smarttool.videodownloader.data.repository

import androidx.lifecycle.LiveData
import com.smarttool.videodownloader.data.dao.HistoryDao
import com.smarttool.videodownloader.data.network.entity.HistoryItem

class HistoryRepositoryImpl  constructor(
    private val historyDao: HistoryDao
) {

    fun getAllHistory(): LiveData<List<HistoryItem>> {
        return historyDao.getHistory()
    }

    suspend fun saveHistory(history: HistoryItem) {
        historyDao.insertHistoryItem(history)

        val itemCount = historyDao.getItemCount()
        if (itemCount > 400) {
            val itemsToDelete = itemCount - 400
            historyDao.deleteOldItems(itemsToDelete)
        }
    }

    fun deleteHistory(history: HistoryItem) {
        historyDao.deleteHistoryItem(history)
    }

    fun deleteAllHistory() {
        historyDao.clear()
    }

    fun queryHistoryItem(
        textSearch: String,
    ): LiveData<List<HistoryItem>> {
        return historyDao
            .getLiveDataHistoryByTextSearch("%" + textSearch.trim() + "%")
    }

    fun queryBookmarkItem(
        textSearch: String,
    ): LiveData<List<HistoryItem>> {
        return historyDao
            .getLiveDataBookmarkByTextSearch("%" + textSearch.trim() + "%")
    }

}