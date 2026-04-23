package com.example.filltracking2

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.filltracking2.data.FileRecord

class FileDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_file_detail)

        val record = intent.getParcelableExtra<FileRecord>("file_record")
        if (record != null) {
            displayFileDetails(record)
        } else {
            Toast.makeText(this, "Error: File record not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupClickListeners()
    }

    private fun displayFileDetails(record: FileRecord) {
        findViewById<TextView>(R.id.tvTitle).text = "File detail"
        findViewById<TextView>(R.id.tvSerial).text = "Internal: ${record.internalSerial}"
        findViewById<TextView>(R.id.tvSubject).text = record.subject
        findViewById<TextView>(R.id.tvSubtitle).text = "Received ${record.dateReceivedGov} · Recipient: ${record.recipientName}"
        
        val tvUrgentBadge = findViewById<TextView>(R.id.tvUrgentBadge)
        if (record.urgency == "Urgent") {
            tvUrgentBadge.visibility = View.VISIBLE
        } else {
            tvUrgentBadge.visibility = View.GONE
        }

        findViewById<TextView>(R.id.tvOriginalSerialDetail).text = record.originalSerial
        findViewById<TextView>(R.id.tvDirectorNoteDetail).text = if (record.notes.isNullOrEmpty()) "No notes" else record.notes

        if (record.attachments.isNotEmpty()) {
            val tvAttachment = findViewById<TextView>(R.id.tvAttachmentName)
            tvAttachment.text = record.attachments.first().name
            tvAttachment.setOnClickListener {
                showToast("Opening ${record.attachments.first().name}...")
            }
        } else {
            findViewById<View>(R.id.cvAttachments).visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
