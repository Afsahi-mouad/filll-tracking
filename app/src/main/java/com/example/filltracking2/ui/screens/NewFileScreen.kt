package com.example.filltracking2.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.filltracking2.data.Attachment
import com.example.filltracking2.data.AttachmentStorage
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.ui.theme.StatusUrgent
import com.example.filltracking2.ui.viewmodel.FileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewFileScreen(
    viewModel: FileViewModel,
    onNavigateBack: () -> Unit
) {
    var originalSerial by remember { mutableStateOf("") }
    var internalSerial by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedUrgency by remember { mutableStateOf("Normal") }
    var selectedSectors by remember { mutableStateOf(setOf<String>()) }
    
    // Date fields
    val today = FileViewModel.getCurrentDate()
    var dateReceivedGov by remember { mutableStateOf(today) }
    var dateRegistered by remember { mutableStateOf(today) }
    var dateDeliveredToDomain by remember { mutableStateOf(today) }

    // Attachments state — stores lightweight metadata + URI
    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var tempCameraAttachment by remember { mutableStateOf<Attachment?>(null) }
    var attachmentError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error via snackbar
    LaunchedEffect(attachmentError) {
        attachmentError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            attachmentError = null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        // Copy each selected file to permanent internal storage (like IndexedDB)
        val savedAttachments = mutableListOf<Attachment>()
        var failCount = 0
        uris.forEach { uri ->
            try {
                val attachment = AttachmentStorage.copyToInternalStorage(context, uri)
                if (attachment != null) {
                    savedAttachments.add(attachment)
                } else {
                    failCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
                failCount++
            }
        }
        attachments = attachments + savedAttachments
        if (failCount > 0) {
            attachmentError = "Could not save $failCount attachment(s). Storage may be full."
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraAttachment?.let { attachment ->
                // Update size and add to list
                attachments = attachments + AttachmentStorage.updateAttachmentSize(attachment)
            }
        }
    }

    /**
     * Creates a camera file in PERMANENT internal storage.
     */
    fun prepareCameraCapture(): Uri {
        val (file, attachment) = AttachmentStorage.createCameraAttachment(context)
        tempCameraAttachment = attachment
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    val sectors = listOf("Finance", "Technical", "HR", "Security", "Admin", "Legal")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New File Record", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Urgency Toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedUrgency == "Normal",
                    onClick = { selectedUrgency = "Normal" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Normal")
                }
                SegmentedButton(
                    selected = selectedUrgency == "Urgent",
                    onClick = { selectedUrgency = "Urgent" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Urgent")
                }
            }

            // Identification Section
            Text("Identification", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            
            OutlinedTextField(
                value = originalSerial,
                onValueChange = { originalSerial = it },
                label = { Text("Original Serial (from Gov) *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = internalSerial,
                onValueChange = { internalSerial = it },
                label = { Text("Internal App Serial *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Dates Section
            Text("Tracking Dates", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateReceivedGov,
                    onValueChange = { dateReceivedGov = it },
                    label = { Text("Received Gov") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = dateDeliveredToDomain,
                    onValueChange = { dateDeliveredToDomain = it },
                    label = { Text("Delivered Sector") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Recipient & Subject
            OutlinedTextField(
                value = recipientName,
                onValueChange = { recipientName = it },
                label = { Text("Recipient Name *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            // Sectors Selection
            Text("Destination Domains", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp
            ) {
                sectors.forEach { sector ->
                    FilterChip(
                        selected = sector in selectedSectors,
                        onClick = {
                            selectedSectors = if (sector in selectedSectors) {
                                selectedSectors - sector
                            } else {
                                selectedSectors + sector
                            }
                        },
                        label = { Text(sector) }
                    )
                }
            }

            // Attachments Section
            Text("Attachments (${attachments.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            
            if (attachments.isNotEmpty()) {
                ScrollableRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    attachments.forEach { attachment ->
                        AttachmentPreview(
                            attachment = attachment,
                            onRemove = {
                                attachments = attachments - attachment
                                // Clean up the file from disk
                                AttachmentStorage.deleteAttachment(Uri.parse(attachment.uri))
                            }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        try {
                            val uri = prepareCameraCapture()
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            attachmentError = "Could not open camera: ${e.message}"
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Camera")
                }
                
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery")
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Director's Notes") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp)
            )
            
            // Save Button - wrapped in try/catch to prevent crash
            Button(
                onClick = {
                    if (originalSerial.isNotBlank() && internalSerial.isNotBlank()) {
                        try {
                            val newRecord = FileRecord(
                                originalSerial = originalSerial,
                                internalSerial = internalSerial,
                                dateReceivedGov = dateReceivedGov,
                                dateRegistered = dateRegistered,
                                dateDeliveredToDomain = dateDeliveredToDomain,
                                recipientName = recipientName,
                                status = "Received",
                                attachments = attachments,
                                subject = subject,
                                urgency = selectedUrgency,
                                sectors = selectedSectors.toList(),
                                notes = notes
                            )
                            viewModel.addRecord(newRecord)
                            onNavigateBack()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            attachmentError = "Could not save document: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedUrgency == "Urgent") StatusUrgent else MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Register Document", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AttachmentPreview(attachment: Attachment, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(80.dp)) {
        AsyncImage(
            model = attachment.uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeholders = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        var rowWidth = 0
        var rowHeight = 0
        var totalHeight = 0
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()

        placeholders.forEach { placeable ->
            if (rowWidth + placeable.width + mainAxisSpacing.toPx() > constraints.maxWidth) {
                rows.add(currentRow)
                totalHeight += rowHeight + crossAxisSpacing.toPx().toInt()
                currentRow = mutableListOf()
                rowWidth = 0
                rowHeight = 0
            }
            currentRow.add(placeable)
            rowWidth += placeable.width + mainAxisSpacing.toPx().toInt()
            rowHeight = maxOf(rowHeight, placeable.height)
        }
        rows.add(currentRow)
        totalHeight += rowHeight

        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                var maxHeight = 0
                row.forEach { placeable ->
                    placeable.place(x, y)
                    x += placeable.width + mainAxisSpacing.toPx().toInt()
                    maxHeight = maxOf(maxHeight, placeable.height)
                }
                y += maxHeight + crossAxisSpacing.toPx().toInt()
            }
        }
    }
}

@Composable
fun ScrollableRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
    ) {
        item { content() }
    }
}
