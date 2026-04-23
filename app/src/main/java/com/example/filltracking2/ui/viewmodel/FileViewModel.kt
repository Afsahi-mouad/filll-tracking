package com.example.filltracking2.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.data.FileRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRecordRepository(application)
    val records: StateFlow<List<FileRecord>> = FileRecordRepository.recordsFlow

    // Error state for UI to observe
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    init {
        loadRecords()
    }

    /**
     * Load records from persistent storage.
     * Prevents data loss by NOT overwriting if load fails partially.
     */
    private fun loadRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val saved = repository.loadRecords()
                if (saved.isNotEmpty()) {
                    FileRecordRepository.updateRecords(saved)
                } else if (!repository.hasAnySavedRecords()) {
                    // ONLY load mock data if this is truly the first run
                    val mockData = listOf(
                        FileRecord(
                            originalSerial = "26/0001",
                            internalSerial = "26/1234",
                            dateReceivedGov = "Apr 10, 2026",
                            dateRegistered = "Apr 10, 2026",
                            dateDeliveredToDomain = "Apr 11, 2026",
                            recipientName = "John Doe",
                            status = "Received",
                            subject = "Annual budget review",
                            urgency = "Urgent",
                            sectors = listOf("Finance"),
                            notes = "Please review by EOD"
                        ),
                        FileRecord(
                            originalSerial = "26/0002",
                            internalSerial = "26/5678",
                            dateReceivedGov = "Apr 12, 2026",
                            dateRegistered = "Apr 12, 2026",
                            dateDeliveredToDomain = "Apr 13, 2026",
                            recipientName = "Alice Smith",
                            status = "Pending",
                            subject = "Maintenance request",
                            urgency = "Normal",
                            sectors = listOf("Technical"),
                            notes = "Check the AC unit"
                        )
                    )
                    FileRecordRepository.updateRecords(mockData)
                    repository.saveRecords(mockData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _saveError.value = "Error loading history: ${e.localizedMessage}"
                }
            }
        }
    }

    /**
     * Refresh records from the in-memory repository.
     * (Deprecated: recordsFlow is now automatic)
     */
    fun refreshRecords() {
        // No longer needed but kept for compatibility
    }

    /**
     * Add a new record and persist immediately.
     * Persistence happens on a background thread to prevent UI lag or crash.
     */
    fun addRecord(record: FileRecord) {
        // Update in-memory repository (and UI via Flow)
        FileRecordRepository.addRecordAtStart(record)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Save ONLY the new record file (efficient storage like IndexedDB)
                val success = repository.saveRecord(record)
                if (!success) {
                    withContext(Dispatchers.Main) {
                        _saveError.value = "Storage is full or inaccessible. Document saved in memory only."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _saveError.value = "Critical save error: ${e.localizedMessage}"
                }
            }
        }
    }

    fun getRecordBySerial(serial: String): FileRecord? {
        return records.value.find { it.internalSerial == serial || it.originalSerial == serial }
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    companion object {
        fun getCurrentDate(): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}
