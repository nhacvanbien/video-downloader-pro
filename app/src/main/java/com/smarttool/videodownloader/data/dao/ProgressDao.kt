package com.smarttool.videodownloader.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM ProgressInfo")
    fun getProgressInfos(): Flow<List<ProgressInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProgressInfo(progressInfo: ProgressInfo)

    @Delete
    fun deleteProgressInfo(progressInfo: ProgressInfo)
}