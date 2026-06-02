package com.example.filltracking2.util

import android.app.Activity
import android.content.ClipData
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.filltracking2.R
import com.example.filltracking2.data.Attachment
import java.io.File
import java.util.Locale

object AttachmentOpener {

    fun isPdf(attachment: Attachment): Boolean {
        return attachment.type.equals("application/pdf", ignoreCase = true) ||
            attachment.name.lowercase(Locale.ROOT).endsWith(".pdf")
    }

    fun openPdf(context: Context, attachment: Attachment): Boolean {
        val file = File(attachment.path)
        if (!file.exists()) {
            return false
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            clipData = ClipData.newUri(context.contentResolver, attachment.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            val chooser = Intent.createChooser(intent, context.getString(R.string.open_pdf_with)).apply {
                clipData = intent.clipData
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: RuntimeException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
