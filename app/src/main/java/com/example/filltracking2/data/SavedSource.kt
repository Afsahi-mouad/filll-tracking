package com.example.filltracking2.data

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class SavedSource(
    val id: String = UUID.randomUUID().toString(),
    val sourceName: String,
    val lastUsedAt: Long = System.currentTimeMillis(),
    val useCount: Int = 1
)
