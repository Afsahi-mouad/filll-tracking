package com.example.filltracking2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.data.FileRecordRepository
import java.io.File

class FileDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_file_detail)

        val recordIndex = intent.getIntExtra("record_index", -1)
        val record = FileRecordRepository.records.getOrNull(recordIndex)
        
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

        val container = findViewById<LinearLayout>(R.id.attachmentsContainer)
        container.removeAllViews()

        if (record.attachments.isNotEmpty()) {
            record.attachments.forEachIndexed { index, attachment ->
                val tv = TextView(this)
                tv.text = "${index + 1}. ${attachment.name}"
                tv.setPadding(0, 16, 0, 16)
                tv.setTextColor(resources.getColor(R.color.text_dark, null))
                tv.textSize = 14f
                tv.setOnClickListener {
                    val file = File(attachment.path)
                    if (file.exists()) {
                        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(uri, attachment.type)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
                    }
                }
                container.addView(tv)
            }
        } else {
            findViewById<View>(R.id.cvAttachments).visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}
