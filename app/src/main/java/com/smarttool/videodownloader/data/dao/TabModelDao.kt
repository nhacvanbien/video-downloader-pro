package com.smarttool.videodownloader.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel

@Dao
interface TabModelDao {

    @Query("SELECT * FROM TabModel ORDER BY _is_selected DESC, _last_accessed DESC")
    fun getAllTabModel(): LiveData<List<TabModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTabModel(item: TabModel)

    @Delete
    suspend fun deleteTabModel(item: TabModel)

    @Query(
        "UPDATE TabModel SET _url = :newUrl, _is_selected = :isSelected, _favicon = :favicon, " +
            "_last_accessed = :lastAccessedAt WHERE _id = :id"
    )
    suspend fun updateInfoTabModel(
        id: String,
        newUrl: String,
        favicon: ByteArray?,
        isSelected: Boolean,
        lastAccessedAt: Long
    )

    @Query("SELECT COUNT(*) FROM TabModel")
    suspend fun getItemCount(): Int

    @Query("SELECT * FROM TabModel WHERE _is_selected = 1 LIMIT 1")
    suspend fun getSelectedTabModel(): TabModel?

    @Query("UPDATE TabModel SET _is_selected = 0 WHERE _is_selected = 1")
    suspend fun updateAllTabsToUnselected()

    @Transaction
    suspend fun insertAndUnselectOthers(item: TabModel) {
        updateAllTabsToUnselected()
        insertTabModel(item)
    }

    @Transaction
    suspend fun updateAndUnselectOthers(
        id: String,
        newUrl: String,
        favicon: ByteArray?,
        isSelected: Boolean,
        lastAccessedAt: Long = System.currentTimeMillis()
    ) {
        updateAllTabsToUnselected()
        updateInfoTabModel(id, newUrl, favicon, isSelected, lastAccessedAt)
    }

    @Query("SELECT * FROM TabModel WHERE _id = :id")
    fun findVideoTaskItemByName(id: String): TabModel

    @Query("DELETE FROM TabModel")
    suspend fun clear()

}