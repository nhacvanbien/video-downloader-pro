package com.smarttool.videodownloader.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smarttool.videodownloader.data.network.entity.AdHost

@Dao
interface AdHostDao {

    @Query("SELECT * FROM AdHost")
    suspend fun getAdHosts(): List<AdHost>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdHosts(adHosts: Set<AdHost>)
}
