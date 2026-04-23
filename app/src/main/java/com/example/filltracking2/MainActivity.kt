package com.example.filltracking2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.filltracking2.ui.theme.FillTrackingTheme
import com.example.filltracking2.data.FileRecordRepository

class MainActivity : ComponentActivity() {
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
            FileRecordRepository.addRecordAtStart(record)
            FileRecordRepository.pendingRecord = null
        }
    }
}