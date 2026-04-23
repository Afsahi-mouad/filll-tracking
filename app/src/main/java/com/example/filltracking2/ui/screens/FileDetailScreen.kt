package com.example.filltracking2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tag
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
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.filltracking2.ui.viewmodel.FileViewModel

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
                title = { Text("Document Details", fontWeight = FontWeight.Bold) },
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
                Text("Document not found")
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
                // Status and Urgency Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusPill(status = record.status, urgency = record.urgency)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = record.sectors.joinToString(", "),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = record.subject,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                // Information Grid
                InfoRow(icon = Icons.Default.Tag, label = "Internal Serial", value = record.internalSerial)
                InfoRow(icon = Icons.Default.Tag, label = "Original Serial", value = record.originalSerial)
                InfoRow(icon = Icons.Default.Person, label = "Recipient", value = record.recipientName)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tracking Timeline", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                
                InfoRow(icon = Icons.Default.CalendarToday, label = "Received (Gov)", value = record.dateReceivedGov)
                InfoRow(icon = Icons.Default.CalendarToday, label = "Registered in App", value = record.dateRegistered)
                InfoRow(icon = Icons.Default.CalendarToday, label = "Delivered to Sector", value = record.dateDeliveredToDomain)

                if (record.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Director's Notes", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = record.notes,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Attachments
                if (record.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Attachments (${record.attachments.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        record.attachments.forEach { attachment ->
                            // Use SubcomposeAsyncImage for better error handling and loading states
                            SubcomposeAsyncImage(
                                model = attachment.uri,
                                contentDescription = attachment.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Fit,
                                loading = {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                },
                                error = {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.errorContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                painter = painterResource(android.R.drawable.stat_notify_error),
                                                contentDescription = "Error",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                "Could not load attachment",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            )
                            Text(
                                text = "${attachment.name} (${String.format("%.1f", attachment.size / 1024.0)} KB)",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 4.dp)
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
