package com.giftclaimer.app.data.dao

import androidx.room.*
import com.giftclaimer.app.data.models.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE isActive = 1")
    suspend fun getActiveProfiles(): List<Profile>

    @Query("SELECT COUNT(*) FROM profiles WHERE isActive = 1")
    fun getActiveProfileCount(): Flow<Int>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile): Long

    @Update
    suspend fun update(profile: Profile)

    @Delete
    suspend fun delete(profile: Profile)
}
