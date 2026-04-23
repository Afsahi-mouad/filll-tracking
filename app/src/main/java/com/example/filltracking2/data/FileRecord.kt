package com.example.filltracking2.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class Attachment(
    val name: String,
    val type: String,
    val size: Long,
    val path: String
) : Parcelable

@Parcelize
data class FileRecord(
    val id: String = UUID.randomUUID().toString(),
    val originalSerial: String,
    val internalSerial: String,
    val dateReceivedGov: String, // Date received from government
    val dateRegistered: String, // Date of registration in the app
    val dateDeliveredToDomain: String, // Date delivered to the domain
    val recipientName: String,
    val status: String, // "Received", "Pending", "Processed"
    val attachments: List<Attachment> = emptyList(), // Store structured attachment info
    val subject: String,
    val urgency: String, // "Normal", "Urgent"
    val sectors: List<String>,
    val notes: String = ""
) : Parcelable

enum class FileStatus {
    RECEIVED, PENDING, PROCESSED
}

enum class UrgencyLevel {
    NORMAL, URGENT
}
