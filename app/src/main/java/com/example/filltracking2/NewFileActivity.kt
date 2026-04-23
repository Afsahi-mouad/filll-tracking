package com.example.filltracking2

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.filltracking2.data.Attachment
import com.example.filltracking2.data.AttachmentStorage
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.data.FileRecordRepository
import java.io.File
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewFileActivity : AppCompatActivity() {

    private val attachments = mutableListOf<Attachment>()
    private var currentCameraFile: File? = null
    private var currentCameraAttachment: Attachment? = null
    private var selectedUrgency = "Normal"
    private val internalSerial = "26/0005" // Mock generated serial

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && currentCameraAttachment != null) {
            val updatedAttachment = AttachmentStorage.updateAttachmentSize(currentCameraAttachment!!)
            attachments.add(updatedAttachment)
            showToast("Document scanned successfully")
            updateAttachmentPreview()
        }
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val internalAttachment = AttachmentStorage.copyToInternalStorage(this, uri)
            if (internalAttachment != null) {
                attachments.add(internalAttachment)
                showToast("File attached: ${internalAttachment.name}")
                updateAttachmentPreview()
            } else {
                showToast("Failed to copy file")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_file)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.tvUrgencyNormal).setOnClickListener {
            updateUrgencyUI(isUrgent = false)
            showToast("Selected: Normal Urgency")
        }

        findViewById<TextView>(R.id.tvUrgencyUrgent).setOnClickListener {
            selectedUrgency = "Urgent"
            updateUrgencyUI(isUrgent = true)
            showToast("Selected: Urgent Urgency")
        }

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            saveFile()
        }

        val cgSectors = findViewById<ChipGroup>(R.id.cgSectors)
        for (i in 0 until cgSectors.childCount) {
            val chip = cgSectors.getChildAt(i) as? Chip
            chip?.setOnClickListener {
                showToast("Sector clicked: ${chip.text}")
            }
        }

        findViewById<CardView>(R.id.cvAttachment).setOnClickListener {
            showAttachmentOptions()
        }
    }

    private fun updateUrgencyUI(isUrgent: Boolean) {
        val tvNormal = findViewById<TextView>(R.id.tvUrgencyNormal)
        val tvUrgent = findViewById<TextView>(R.id.tvUrgencyUrgent)

        if (isUrgent) {
            tvUrgent.setBackgroundResource(R.drawable.bg_stat_card)
            tvUrgent.setTextColor(ContextCompat.getColor(this, R.color.white))
            tvNormal.setBackgroundResource(R.drawable.bg_search_bar)
            tvNormal.setTextColor(ContextCompat.getColor(this, R.color.text_gray))
        } else {
            tvNormal.setBackgroundResource(R.drawable.bg_stat_card)
            tvNormal.setTextColor(ContextCompat.getColor(this, R.color.white))
            tvUrgent.setBackgroundResource(R.drawable.bg_search_bar)
            tvUrgent.setTextColor(ContextCompat.getColor(this, R.color.text_gray))
        }
    }

    private fun updateAttachmentPreview() {
        val tvAttachmentName = findViewById<TextView>(R.id.tvAttachmentName)
        if (attachments.isNotEmpty()) {
            tvAttachmentName.text = "${attachments.size} attachment(s) added"
        } else {
            tvAttachmentName.text = "Add attachment (Optional)"
        }
    }

    private fun saveFile() {
        val originalSerial = findViewById<EditText>(R.id.etOriginalSerial).text.toString()
        val recipientName = findViewById<EditText>(R.id.etRecipientName).text.toString()
        val subject = findViewById<EditText>(R.id.etSubject).text.toString()
        val notes = findViewById<EditText>(R.id.etNotes).text.toString()

        if (originalSerial.isEmpty() || recipientName.isEmpty()) {
            showToast("Please fill required fields (Serial & Recipient)")
            return
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateReceived = dateFormat.format(Date())

        val record = FileRecord(
            originalSerial = originalSerial,
            internalSerial = internalSerial,
            dateReceivedGov = dateReceived,
            dateRegistered = dateReceived,
            dateDeliveredToDomain = "",
            recipientName = recipientName,
            status = "Received",
            attachments = attachments.toList(),
            subject = subject,
            urgency = selectedUrgency,
            sectors = listOf("Finance"), // Simplified for demo
            notes = notes
        )

        try {
            FileRecordRepository.pendingRecord = record
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to save record: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showAttachmentOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Attachment")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    val (file, attachment) = AttachmentStorage.createCameraAttachment(this)
                    currentCameraFile = file
                    currentCameraAttachment = attachment
                    
                    val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    val photoURI = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider",
                        file
                    )
                    takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    cameraLauncher.launch(takePicture)
                }
                1 -> {
                    filePickerLauncher.launch("image/*")
                }
                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }
}