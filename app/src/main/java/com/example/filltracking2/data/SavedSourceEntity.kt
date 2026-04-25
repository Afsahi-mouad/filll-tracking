package com.example.filltracking2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_sources")
data class SavedSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceName: String,
    val lastUsedAt: Long = System.currentTimeMillis(),
    val useCount: Int = 1
)
