package com.giftclaimer.app.data.dao

import androidx.room.*
import com.giftclaimer.app.data.models.ClaimLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ClaimLogDao {
    @Query("SELECT * FROM claim_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<ClaimLog>>

    @Query("SELECT COUNT(*) FROM claim_logs WHERE result = 'SUCCESS' AND timestamp > :startOfDay")
    fun getTodaySuccessCount(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM claim_logs WHERE timestamp > :startOfDay")
    fun getTodayTotalCount(startOfDay: Long): Flow<Int>

    @Insert
    suspend fun insert(log: ClaimLog)

    @Insert
    suspend fun insertAll(logs: List<ClaimLog>)

    @Query("DELETE FROM claim_logs")
    suspend fun deleteAll()
}
