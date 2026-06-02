package com.example.filltracking2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.filltracking2.R
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.ui.theme.*
import com.example.filltracking2.ui.viewmodel.FileViewModel
import com.example.filltracking2.util.AttachmentOpener
import java.io.File

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FileDetailScreen(
    serial: String,
    viewModel: FileViewModel,
    onNavigateBack: () -> Unit,
    onEditFile: (String) -> Unit,
    onOpenImageViewer: () -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
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

    val backgroundColor = if (isDark) Color.Black else MoroccoSurface
    val cardColor = if (isDark) DarkSurface else Color.White
    val primaryTextColor = if (isDark) AccentGold else Color(0xFF212121)
    val secondaryTextColor = if (isDark) TextSecondaryDark else Color(0xFF757575)
    val accentColor = if (isDark) AccentGold else MoroccoPrimary

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = primaryTextColor,
                    navigationIconContentColor = primaryTextColor,
                    actionIconContentColor = primaryTextColor
                ),
                title = { Text(stringResource(R.string.doc_details), fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (record != null) {
                        IconButton(onClick = { onEditFile(record.internalSerial) }) {
                            Icon(Icons.Default.Edit, stringResource(R.string.edit), tint = if (isDark) AccentGold else MoroccoPrimary)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = Color(0xFFB71F27))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (record == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.doc_not_found), color = primaryTextColor)
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
                // Serial Numbers Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = if (isDark) BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f)) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.internal_serial_label), style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                                Text(record.internalSerial, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (isDark) AccentGold else MoroccoPrimary)
                            }
                            VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 16.dp), color = if (isDark) AccentGold.copy(alpha = 0.3f) else Color.LightGray)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.original_serial_label), style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                                Text(record.originalSerial, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (isDark) AccentGold else MoroccoPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        DetailStatusPill(status = record.status, urgency = record.urgency, isDark = isDark)
                    }
                }

                // Document Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = if (isDark) BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f)) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = record.subject, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = primaryTextColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = if (isDark) AccentGold.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Business, null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${stringResource(R.string.source_label)}: ", style = MaterialTheme.typography.bodyMedium, color = secondaryTextColor)
                            Text(record.source, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.NearMe, null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(stringResource(R.string.destination_sectors), style = MaterialTheme.typography.bodyMedium, color = secondaryTextColor)
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.foundation.layout.FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    record.sectors.forEach { sector ->
                                        Surface(
                                            color = if (isDark) AccentGold.copy(alpha = 0.15f) else MoroccoPrimary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = sector,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = accentColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Recipient Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = if (isDark) BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f)) else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = if (isDark) AccentGold else MoroccoPrimary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val initials = record.recipientName.split(" ").filter { it.isNotBlank() }.take(2).map { it.first() }.joinToString("").uppercase()
                                Text(initials, color = if (isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.recipient), style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                            Text(record.recipientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                        }
                    }
                }

                // Timeline Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = if (isDark) BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f)) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.tracking_timeline),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) AccentGold else MoroccoPrimary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        TimelineItem(
                            label = stringResource(R.string.received_gov_label),
                            date = record.dateReceivedGov,
                            color = Color(0xFFFFA726),
                            isLast = false,
                            isDark = isDark
                        )
                        TimelineItem(
                            label = stringResource(R.string.registered_label),
                            date = record.dateRegistered,
                            color = Color(0xFF004824),
                            isLast = false,
                            isDark = isDark
                        )
                        TimelineItem(
                            label = stringResource(R.string.delivered_label),
                            date = record.dateDeliveredToDomain,
                            color = Color(0xFF66BB6A),
                            isLast = true,
                            isDark = isDark
                        )
                    }
                }

                // Attachments Section
                if (record.attachments.isNotEmpty()) {
                    Text(
                        "${stringResource(R.string.attachments)} (${record.attachments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    val context = LocalContext.current
                    val errorOpenPdf = stringResource(R.string.error_open_pdf)
                    
                    record.attachments.forEach { attachment ->
                        if (AttachmentOpener.isPdf(attachment)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!AttachmentOpener.openPdf(context, attachment)) {
                                            android.widget.Toast.makeText(context, errorOpenPdf, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                color = if (isDark) Color(0xFF1A1A1A) else Color(0xFFB2DFDB),
                                shape = RoundedCornerShape(16.dp),
                                border = if (isDark) BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f)) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        null,
                                        tint = if (isDark) AccentGold else Color(0xFF00695C),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            attachment.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            color = if (isDark) Color.White else Color(0xFF004D40)
                                        )
                                        Text(
                                            "PDF Document",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDark) AccentGold.copy(alpha = 0.7f) else Color(0xFF00695C).copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        } else {
                            SubcomposeAsyncImage(
                                model = File(attachment.path),
                                contentDescription = attachment.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isDark) Color(0xFF1A1A1A) else Color.LightGray)
                                    .clickable {
                                        val imageAttachments = record.attachments.filterNot(AttachmentOpener::isPdf)
                                        val imagePaths = imageAttachments.map { it.path }
                                        val imageIndex = imageAttachments.indexOf(attachment)
                                        
                                        viewModel.openImageViewer(imagePaths, imageIndex)
                                        onOpenImageViewer()
                                    },
                                contentScale = ContentScale.Crop,
                                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accentColor) } },
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
fun DetailStatusPill(status: String, urgency: String, isDark: Boolean) {
    val displayStatus = if (status == "Pending") "Received" else status
    val color = when {
        urgency == "Urgent" -> StatusUrgent
        displayStatus == "Processed" -> StatusProcessed
        else -> StatusReceived
    }
    
    Surface(
        color = if (isDark) Color.Transparent else color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Inbox, null, tint = color, modifier = Modifier.size(14.dp))
            Text(
                if (urgency == "Urgent") "URGENT" else displayStatus.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun TimelineItem(label: String, date: String, color: Color, isLast: Boolean, isDark: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().height(intrinsicSize = IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(if (isDark) AccentGold.copy(alpha = 0.3f) else Color.LightGray)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 24.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (isDark) TextSecondaryDark else Color(0xFF757575))
            Text(date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF212121))
        }
    }
}
