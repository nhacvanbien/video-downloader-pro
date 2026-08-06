package com.smarttool.videodownloader.data.dao

import androidx.room.*
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import io.reactivex.rxjava3.core.Flowable

@Dao
interface ProgressDao {

    @Query("SELECT * FROM ProgressInfo")
    fun getProgressInfos(): Flowable<List<ProgressInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProgressInfo(progressInfo: ProgressInfo)

    @Delete
    fun deleteProgressInfo(progressInfo: ProgressInfo)
}