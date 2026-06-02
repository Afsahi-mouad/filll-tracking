package com.example.filltracking2

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.filltracking2.data.Attachment
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
        // Serials
        findViewById<TextView>(R.id.tvInternalSerialDetail).text = record.internalSerial
        findViewById<TextView>(R.id.tvOriginalSerialDetail).text = record.originalSerial
        
        // Status Badge
        val tvStatusBadge = findViewById<TextView>(R.id.tvStatusBadge)
        tvStatusBadge.text = record.status.uppercase()
        
        // Subject and Source
        findViewById<TextView>(R.id.tvSubjectDetail).text = record.subject
        findViewById<TextView>(R.id.tvSourceDetail).text = "Source: ${record.source}"
        
        // Sectors Badge
        val tvDestinationBadge = findViewById<TextView>(R.id.tvDestinationBadge)
        if (record.sectors.isNotEmpty()) {
            tvDestinationBadge.text = record.sectors.joinToString(", ")
            tvDestinationBadge.visibility = View.VISIBLE
        } else {
            tvDestinationBadge.visibility = View.GONE
        }

        // Recipient
        findViewById<TextView>(R.id.tvRecipientNameDetail).text = record.recipientName
        val tvInitial = findViewById<TextView>(R.id.tvRecipientInitial)
        tvInitial.text = if (record.recipientName.isNotEmpty()) {
            record.recipientName.split(" ").filter { it.isNotEmpty() }.take(2).map { it[0] }.joinToString("")
        } else "?"

        // Timeline
        setupTimeline(record)

        // Attachments
        setupAttachments(record)
    }

    private fun setupTimeline(record: FileRecord) {
        val container = findViewById<LinearLayout>(R.id.timelineContainer)
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)

        // Timeline items
        val steps = listOf(
            Triple("Reçu (Gouv)", record.dateReceivedGov, "#EF6C00"),
            Triple("Enregistré dans l'app", record.dateRegistered, "#1B5E20"),
            Triple("Livré au secteur", record.dateDeliveredToDomain, "#4CAF50")
        )

        steps.forEachIndexed { index, step ->
            val view = inflater.inflate(R.layout.item_timeline, container, false)
            view.findViewById<TextView>(R.id.tvTimelineTitle).text = step.first
            view.findViewById<TextView>(R.id.tvTimelineDate).text = step.second
            
            // Adjust dots and lines
            val dot = view.findViewById<View>(R.id.vDot)
            dot.setBackgroundResource(R.drawable.bg_circle_green)
            dot.background.setTint(Color.parseColor(step.third))
            
            if (index == 0) {
                view.findViewById<View>(R.id.vTopLine).visibility = View.INVISIBLE
            }
            if (index == steps.size - 1) {
                view.findViewById<View>(R.id.vBottomLine).visibility = View.INVISIBLE
            }
            
            container.addView(view)
        }
    }

    private fun setupAttachments(record: FileRecord) {
        val header = findViewById<TextView>(R.id.tvAttachmentsHeader)
        header.text = "Pièces jointes (${record.attachments.size})"

        val container = findViewById<LinearLayout>(R.id.attachmentsContainerDetail)
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)

        if (record.attachments.isNotEmpty()) {
            record.attachments.forEach { attachment ->
                val view = inflater.inflate(R.layout.item_attachment_card, container, false)
                view.findViewById<TextView>(R.id.tvAttachmentName).text = attachment.name
                view.findViewById<TextView>(R.id.tvAttachmentType).text = if (attachment.type.contains("pdf")) "PDF Document" else "Image File"
                
                val icon = view.findViewById<ImageView>(R.id.ivAttachmentIcon)
                if (attachment.type.contains("pdf")) {
                    icon.setImageResource(R.drawable.ic_pdf)
                } else {
                    icon.setImageResource(R.drawable.ic_image)
                }

                view.setOnClickListener {
                    openAttachment(attachment)
                }
                container.addView(view)
            }
            
            // Show image preview if first attachment is an image
            val firstImage = record.attachments.find { !it.type.contains("pdf") }
            if (firstImage != null) {
                findViewById<View>(R.id.cvImagePreview).visibility = View.VISIBLE
                val ivPreview = findViewById<ImageView>(R.id.ivDetailPreview)
                val file = File(firstImage.path)
                if (file.exists()) {
                    ivPreview.setImageURI(Uri.fromFile(file))
                }
            }
        }
    }

    private fun openAttachment(attachment: Attachment) {
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

    private fun setupClickListeners() {
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
        findViewById<View>(R.id.btnEdit).setOnClickListener {
            Toast.makeText(this, "Edit functionality coming soon", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btnDelete).setOnClickListener {
            Toast.makeText(this, "Delete functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}
