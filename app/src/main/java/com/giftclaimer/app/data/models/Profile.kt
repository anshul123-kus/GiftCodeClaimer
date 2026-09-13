package com.giftclaimer.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val label: String = "",
    val dataDirSuffix: String = "",
    val isLoggedIn: Boolean = false,
    val isActive: Boolean = true
)
