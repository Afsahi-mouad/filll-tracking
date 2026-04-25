package com.example.filltracking2.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File


class FileRecordRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val database = AppDatabase.getDatabase(context)
    val savedSourceDao = database.savedSourceDao()

    companion object {
        private const val PREFS_NAME = "fill_tracking_records"
        private const val KEY_RECORDS = "records_json"
        private const val RECORDS_DIR = "app_records"
        private const val SOURCES_DIR = "app_sources"

        // Singleton-like in-memory store to avoid Intent size limits
        private val _recordsFlow = MutableStateFlow<List<FileRecord>>(emptyList())
        val recordsFlow: StateFlow<List<FileRecord>> = _recordsFlow.asStateFlow()

        private val _sourcesFlow = MutableStateFlow<List<SavedSource>>(emptyList())
        val sourcesFlow: StateFlow<List<SavedSource>> = _sourcesFlow.asStateFlow()
        
        val records: MutableList<FileRecord>
            get() = _recordsFlow.value.toMutableList()

        var pendingRecord: FileRecord? = null

        fun updateRecords(newList: List<FileRecord>) {
            _recordsFlow.value = newList
        }

        fun addRecordAtStart(record: FileRecord) {
            _recordsFlow.value = listOf(record) + _recordsFlow.value
        }

        fun updateRecordInList(updatedRecord: FileRecord) {
            _recordsFlow.value = _recordsFlow.value.map {
                if (it.id == updatedRecord.id) updatedRecord else it
            }
        }

        fun removeRecordFromList(id: String) {
            _recordsFlow.value = _recordsFlow.value.filter { it.id != id }
        }
    }

    private fun getRecordsDir(): File {
        val dir = File(context.filesDir, RECORDS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getSourcesDir(): File {
        val dir = File(context.filesDir, SOURCES_DIR)
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


    fun saveRecords(records: List<FileRecord>): Boolean {
        var allSuccess = true
        for (record in records) {
            if (!saveRecord(record)) {
                allSuccess = false
            }
        }
        return allSuccess
    }


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


    fun clearAll() {
        try {
            getRecordsDir().deleteRecursively()
            getSourcesDir().deleteRecursively()
            prefs.edit().remove(KEY_RECORDS).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Saved Sources Persistence ---

    fun saveSource(source: SavedSource): Boolean {
        return try {
            val file = File(getSourcesDir(), "source_${source.sourceName.hashCode()}.json")
            val json = gson.toJson(source)
            file.writeText(json)
            loadSources() // Refresh flow
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadSources(): List<SavedSource> {
        val sources = mutableListOf<SavedSource>()
        try {
            val files = getSourcesDir().listFiles() ?: return emptyList()
            for (file in files) {
                if (file.extension == "json") {
                    val json = file.readText()
                    val source = gson.fromJson(json, SavedSource::class.java)
                    if (source != null) {
                        sources.add(source)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val sorted = sources.sortedByDescending { it.lastUsedAt }
        _sourcesFlow.value = sorted
        return sorted
    }
}
