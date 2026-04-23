package com.example.filltracking2.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Persistence layer for FileRecords using individual JSON files in internal storage.
 *
 * Only lightweight data is stored (text fields + file path references).
 * Actual file bytes live on disk in internal storage, managed by AttachmentStorage.
 * This keeps SharedPreferences small and prevents storage overflow crashes.
 */
class FileRecordRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "fill_tracking_records"
        private const val KEY_RECORDS = "records_json"
        private const val RECORDS_DIR = "app_records"
    }

    private fun getRecordsDir(): File {
        val dir = File(context.filesDir, RECORDS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Checks if any records exist on disk.
     */
    fun hasAnySavedRecords(): Boolean {
        return try {
            val dir = getRecordsDir()
            dir.exists() && (dir.listFiles()?.any { it.extension == "json" } == true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Migration: Move data from legacy SharedPreferences to individual files.
     * This is called on load if SharedPreferences contains data.
     */
    private fun migrateIfNecessary() {
        try {
            val json = prefs.getString(KEY_RECORDS, null) ?: return
            val type = object : TypeToken<List<FileRecord>>() {}.type
            val records: List<FileRecord> = gson.fromJson(json, type) ?: return
            
            for (record in records) {
                saveRecord(record)
            }
            
            // Clear legacy data once migrated
            prefs.edit().remove(KEY_RECORDS).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Saves a single record to its own file.
     * Returns true on success.
     */
    fun saveRecord(record: FileRecord): Boolean {
        return try {
            val file = File(getRecordsDir(), "record_${record.id}.json")
            val json = gson.toJson(record)
            file.writeText(json)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Save the full list of records (used by ViewModel).
     * Now saves each record individually to disk.
     */
    fun saveRecords(records: List<FileRecord>): Boolean {
        var allSuccess = true
        for (record in records) {
            if (!saveRecord(record)) {
                allSuccess = false
            }
        }
        return allSuccess
    }

    /**
     * Load all saved records from the records directory.
     * Also performs migration if legacy data exists.
     */
    fun loadRecords(): List<FileRecord> {
        migrateIfNecessary()
        
        val records = mutableListOf<FileRecord>()
        try {
            val files = getRecordsDir().listFiles() ?: return emptyList()
            for (file in files) {
                if (file.extension == "json") {
                    val json = file.readText()
                    val record = gson.fromJson(json, FileRecord::class.java)
                    if (record != null) {
                        records.add(record)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return records.sortedByDescending { it.dateRegistered } // Sort by date or id as needed
    }

    /**
     * Delete a single record file.
     */
    fun deleteRecord(id: String): Boolean {
        return try {
            val file = File(getRecordsDir(), "record_$id.json")
            if (file.exists()) {
                file.delete()
            } else true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Clear all saved records.
     */
    fun clearAll() {
        try {
            getRecordsDir().deleteRecursively()
            prefs.edit().remove(KEY_RECORDS).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
