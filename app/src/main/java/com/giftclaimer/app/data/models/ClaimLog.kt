package com.giftclaimer.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "claim_logs")
data class ClaimLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val siteName: String,
    val profileName: String,
    val result: String,  // SUCCESS, FAILED, POPUP
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
