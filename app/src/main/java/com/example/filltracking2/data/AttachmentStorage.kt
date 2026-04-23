package com.example.filltracking2.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Utility for safely copying attachment files to permanent internal storage.
 * Files are stored in app-private "attachments" directory inside filesDir,
 * which survives app restarts and is not cleared like cacheDir.
 */
object AttachmentStorage {

    private const val ATTACHMENTS_DIR = "attachments"

    /**
     * Returns the permanent attachments directory, creating it if needed.
     */
    private fun getAttachmentsDir(context: Context): File {
        val dir = File(context.filesDir, ATTACHMENTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copies a content:// or file:// URI into permanent internal storage.
     * Returns a new Attachment object on success, or null on failure.
     */
    fun copyToInternalStorage(context: Context, sourceUri: Uri): Attachment? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(sourceUri) ?: return null
            
            // Try to get original file name and size
            var fileName = "attachment_${UUID.randomUUID()}.jpg"
            var fileSize = 0L
            var fileType = "image/jpeg"

            contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }
            
            fileType = contentResolver.getType(sourceUri) ?: "application/octet-stream"

            val destFile = File(getAttachmentsDir(context), "file_${UUID.randomUUID()}_$fileName")
            FileOutputStream(destFile).use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                }
            }
            
            Attachment(
                name = fileName,
                type = fileType,
                size = if (fileSize > 0) fileSize else destFile.length(),
                path = destFile.absolutePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a new empty file in the permanent attachments directory
     * for the camera to write into.
     */
    fun createCameraAttachment(context: Context): Pair<File, Attachment> {
        val fileName = "IMG_${UUID.randomUUID()}.jpg"
        val file = File(getAttachmentsDir(context), fileName)
        file.createNewFile()
        
        val attachment = Attachment(
            name = fileName,
            type = "image/jpeg",
            size = 0, // Will be updated after photo is taken
            path = file.absolutePath
        )
        return Pair(file, attachment)
    }

    /**
     * Updates an attachment's size after a file has been written.
     */
    fun updateAttachmentSize(attachment: Attachment): Attachment {
        return try {
            val file = File(attachment.path)
            attachment.copy(size = file.length())
        } catch (e: Exception) {
            attachment
        }
    }

    /**
     * Deletes an attachment file from internal storage.
     * Safe to call with any URI — will silently ignore non-file URIs.
     */
    fun deleteAttachment(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if an attachment file still exists on disk.
     */
    fun attachmentExists(path: String): Boolean {
        return try {
            File(path).exists()
        } catch (e: Exception) {
            false
        }
    }
}
