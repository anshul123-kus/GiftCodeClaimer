package com.giftclaimer.app.data.dao

import androidx.room.*
import com.giftclaimer.app.data.models.Site
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY id ASC")
    fun getAllSites(): Flow<List<Site>>

    @Query("SELECT * FROM sites WHERE isActive = 1")
    suspend fun getActiveSites(): List<Site>

    @Query("SELECT COUNT(*) FROM sites WHERE isActive = 1")
    fun getActiveSiteCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(site: Site): Long

    @Update
    suspend fun update(site: Site)

    @Delete
    suspend fun delete(site: Site)

    @Query("DELETE FROM sites")
    suspend fun deleteAll()
}
