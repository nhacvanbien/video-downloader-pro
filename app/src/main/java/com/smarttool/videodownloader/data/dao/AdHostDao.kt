package com.smarttool.videodownloader.data.dao

import androidx.room.*
import com.smarttool.videodownloader.data.network.entity.AdHost

@Dao
interface AdHostDao {

    @Query("SELECT COUNT(*) FROM AdHost")
    suspend fun getHostsCount(): Int
    @Query("SELECT * FROM AdHost")
    suspend fun getAdHosts(): List<AdHost>

    @Query("SELECT EXISTS(SELECT * FROM AdHost WHERE host = :host)")
    fun isAdHost(host: String): Boolean

    @Insert(onConflict = OnConflictStrategy.NONE)
    suspend fun insertAdHost(adHost: AdHost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdHosts(adHosts: Set<AdHost>)

    @Delete
    suspend fun deleteAdHosts(adHosts: Set<AdHost>)

    @Query("DELETE FROM AdHost")
    suspend fun deleteAllAdHosts()

    @Delete
    suspend fun deleteAdHost(host: AdHost)

}
