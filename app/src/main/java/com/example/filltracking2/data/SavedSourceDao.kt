package com.example.filltracking2.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSourceDao {
    @Query("SELECT * FROM saved_sources ORDER BY lastUsedAt DESC")
    fun getAllSources(): Flow<List<SavedSourceEntity>>

    @Query("SELECT * FROM saved_sources WHERE LOWER(sourceName) = LOWER(:name) LIMIT 1")
    suspend fun getSourceByName(name: String): SavedSourceEntity?

    @Insert
    suspend fun insert(source: SavedSourceEntity)

    @Update
    suspend fun update(source: SavedSourceEntity)

    @Transaction
    suspend fun insertOrUpdate(sourceName: String) {
        val existing = getSourceByName(sourceName)
        if (existing != null) {
            update(existing.copy(
                lastUsedAt = System.currentTimeMillis(),
                useCount = existing.useCount + 1
            ))
        } else {
            insert(SavedSourceEntity(sourceName = sourceName))
        }
    }
}
