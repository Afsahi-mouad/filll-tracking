package com.example.filltracking2

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.filltracking2.ui.theme.FillTrackingTheme
import com.example.filltracking2.data.FileRecordRepository
import com.example.filltracking2.util.PreferenceManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            FillTrackingTheme {
                FillTrackingApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check for pending records from NewFileActivity (Bug 1 & 2 fix)
        val record = FileRecordRepository.pendingRecord
        if (record != null) {
            val repository = FileRecordRepository(this)
            repository.saveRecord(record) // Save to disk (Bug 3 fix)
            FileRecordRepository.addRecordAtStart(record) // Update UI
            FileRecordRepository.pendingRecord = null
        }
    }
}