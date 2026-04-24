package com.example.filltracking2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import com.example.filltracking2.ui.theme.ThemeManager
import com.example.filltracking2.ui.viewmodel.FileViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailScreen(
    serial: String,
    viewModel: FileViewModel,
    onNavigateBack: () -> Unit
) {
    val records by viewModel.records.collectAsState()
    val record = records.find { it.internalSerial == serial || it.originalSerial == serial }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ThemeManager.getString("doc_details"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (record == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(ThemeManager.getString("doc_not_found"))
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

                InfoRow(icon = Icons.Default.Tag, label = ThemeManager.getString("internal_serial_label"), value = record.internalSerial)
                InfoRow(icon = Icons.Default.Tag, label = ThemeManager.getString("original_serial_label"), value = record.originalSerial)
                InfoRow(icon = Icons.Default.Person, label = ThemeManager.getString("recipient"), value = record.recipientName)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(ThemeManager.getString("tracking_timeline"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                
                InfoRow(icon = Icons.Default.CalendarToday, label = ThemeManager.getString("received_gov_label"), value = record.dateReceivedGov)
                InfoRow(icon = Icons.Default.CalendarToday, label = ThemeManager.getString("registered_label"), value = record.dateRegistered)
                InfoRow(icon = Icons.Default.CalendarToday, label = ThemeManager.getString("delivered_label"), value = record.dateDeliveredToDomain)

                if (record.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(ThemeManager.getString("notes"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
                        Text(text = record.notes, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (record.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${ThemeManager.getString("attachments")} (${record.attachments.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        record.attachments.forEach { attachment ->
                            SubcomposeAsyncImage(
                                model = File(attachment.path),
                                contentDescription = attachment.name,
                                modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)).background(Color.LightGray),
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
