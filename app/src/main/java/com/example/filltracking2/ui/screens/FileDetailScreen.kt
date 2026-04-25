package com.example.filltracking2.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import com.example.filltracking2.R
import com.example.filltracking2.data.Attachment
import com.example.filltracking2.ui.theme.ThemeManager
import com.example.filltracking2.ui.viewmodel.FileViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailScreen(
    serial: String,
    viewModel: FileViewModel,
    onNavigateBack: () -> Unit,
    onEditFile: (String) -> Unit,
    onOpenImageViewer: () -> Unit
) {
    val records by viewModel.records.collectAsState()
    val record = records.find { it.internalSerial == serial || it.originalSerial == serial }
    
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && record != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecord(record)
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.doc_details), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (record != null) {
                        IconButton(onClick = { onEditFile(record.internalSerial) }) {
                            Icon(Icons.Default.Edit, stringResource(R.string.edit))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (record == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.doc_not_found))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusPill(status = record.status, urgency = record.urgency)

                Text(text = record.subject, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                HorizontalDivider()

                InfoRow(icon = Icons.Default.Tag, label = stringResource(R.string.internal_serial_label), value = record.internalSerial)
                InfoRow(icon = Icons.Default.Tag, label = stringResource(R.string.original_serial_label), value = record.originalSerial)
                InfoRow(icon = Icons.Default.Business, label = stringResource(R.string.source_label), value = record.source)
                InfoRow(icon = Icons.Default.Person, label = stringResource(R.string.recipient), value = record.recipientName)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.tracking_timeline), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                
                InfoRow(icon = Icons.Default.CalendarToday, label = stringResource(R.string.received_gov_label), value = record.dateReceivedGov)
                InfoRow(icon = Icons.Default.CalendarToday, label = stringResource(R.string.registered_label), value = record.dateRegistered)
                InfoRow(icon = Icons.Default.CalendarToday, label = stringResource(R.string.delivered_label), value = record.dateDeliveredToDomain)

                if (record.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.notes), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
                        Text(text = record.notes, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (record.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${stringResource(R.string.attachments)} (${record.attachments.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    val context = LocalContext.current
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        record.attachments.forEach { attachment ->
                            if (attachment.type == "application/pdf") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clickable {
                                            try {
                                                val file = File(attachment.path)
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "application/pdf")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Cannot open PDF", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                                        Column {
                                            Text(attachment.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Text("PDF Document", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            } else {
                                SubcomposeAsyncImage(
                                    model = File(attachment.path),
                                    contentDescription = attachment.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.LightGray)
                                        .clickable {
                                            viewModel.openImageViewer(
                                                record.attachments.map { it.path },
                                                record.attachments.indexOf(attachment)
                                            )
                                            onOpenImageViewer()
                                        },
                                    contentScale = ContentScale.Fit,
                                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) } },
                                    error = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer), contentAlignment = Alignment.Center) { Icon(painterResource(android.R.drawable.stat_notify_error), null, tint = MaterialTheme.colorScheme.error) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
