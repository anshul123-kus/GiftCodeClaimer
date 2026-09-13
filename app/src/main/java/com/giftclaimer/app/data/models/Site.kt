package com.giftclaimer.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class Site(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val inputSelector: String,
    val buttonSelector: String,
    val popupSelector: String,
    val successText: String,
    val isActive: Boolean = true
)
