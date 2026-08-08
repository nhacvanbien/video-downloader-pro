package com.smarttool.videodownloader.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.smarttool.videodownloader.data.network.entity.HistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistoryItem(item: HistoryItem)

    @Delete
    fun deleteHistoryItem(historyItem: HistoryItem)

    @Query("DELETE FROM HistoryItem WHERE isBookmark = 0")
    fun clear()

    @Query(
        """SELECT * FROM HistoryItem
       WHERE title LIKE :textSearch  AND isBookmark = 0
       ORDER BY datetime DESC
       """
    )
    fun observeHistoryByTextSearch(textSearch: String?): Flow<List<HistoryItem>>

    @Query(
        """SELECT * FROM HistoryItem
       WHERE title LIKE :textSearch  AND isBookmark = 1
       ORDER BY datetime DESC
       """
    )
    fun observeBookmarkByTextSearch(textSearch: String?): Flow<List<HistoryItem>>

    @Query("DELETE FROM HistoryItem WHERE id IN (SELECT id FROM historyitem ORDER BY id ASC LIMIT :count)")
    suspend fun deleteOldItems(count: Int)

    @Query("SELECT COUNT(*) FROM HistoryItem")
    suspend fun getItemCount(): Int
}