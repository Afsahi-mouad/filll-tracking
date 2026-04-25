package com.example.filltracking2.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filltracking2.R
import com.example.filltracking2.data.Attachment
import com.example.filltracking2.data.AttachmentStorage
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.ui.theme.StatusUrgent
import com.example.filltracking2.ui.theme.ThemeManager
import com.example.filltracking2.ui.viewmodel.FileViewModel
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDate(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewFileScreen(
    viewModel: FileViewModel,
    editingRecordId: String? = null,
    onNavigateBack: () -> Unit,
    onOpenImageViewer: () -> Unit
) {
    val context = LocalContext.current
    val recordToEdit = remember(editingRecordId) {
        editingRecordId?.let { viewModel.getRecordBySerial(it) }
    }

    val savedSources by viewModel.savedSourcesFromRoom.collectAsStateWithLifecycle()
    val defaultSources = listOf(
        R.string.source_prefecture,
        R.string.source_academy,
        R.string.source_directorate,
        R.string.source_health_delegation,
        R.string.source_ministry
    )

    val years = listOf("24", "25", "26", "27")
    
    // Original Serial
    var selectedYear by remember { 
        mutableStateOf(recordToEdit?.originalSerial?.split("/")?.getOrNull(0) ?: "26") 
    }
    var originalSerialDigits by remember { 
        mutableStateOf(recordToEdit?.originalSerial?.split("/")?.getOrNull(1) ?: "") 
    }
    
    // Internal Serial
    var internalYear by remember { 
        mutableStateOf(recordToEdit?.internalSerial?.split("/")?.getOrNull(0) ?: "26") 
    }
    var internalSerialDigits by remember { 
        mutableStateOf(recordToEdit?.internalSerial?.split("/")?.getOrNull(1) ?: "") 
    }

    var yearDropdownExpanded by remember { mutableStateOf(false) }
    var internalYearDropdownExpanded by remember { mutableStateOf(false) }

    var recipientName by remember { mutableStateOf(recordToEdit?.recipientName ?: "") }
    var subject by remember { mutableStateOf(recordToEdit?.subject ?: "") }
    
    // Source State
    var sourceDropdownExpanded by remember { mutableStateOf(false) }
    var selectedSourceText by remember { mutableStateOf(recordToEdit?.source ?: "") }
    var customSourceText by remember { mutableStateOf("") }
    var isOtherSelected by remember { mutableStateOf(false) }
    
    // Determine initial dropdown selection
    LaunchedEffect(recordToEdit, savedSources) {
        recordToEdit?.let { record ->
            val isDefault = defaultSources.any { context.getString(it) == record.source }
            
            if (isDefault) {
                selectedSourceText = record.source
                isOtherSelected = false
            } else if (record.source.isNotEmpty()) {
                val isSavedCustom = savedSources.any { it.sourceName == record.source }
                if (isSavedCustom) {
                    selectedSourceText = record.source
                    isOtherSelected = false
                } else {
                    // It was a custom source but not in Room yet (or typed via Other)
                    selectedSourceText = context.getString(R.string.source_other)
                    customSourceText = record.source
                    isOtherSelected = true
                }
            }
        }
    }

    var notes by remember { mutableStateOf(recordToEdit?.notes ?: "") }
    var selectedUrgency by remember { mutableStateOf(recordToEdit?.urgency ?: "Normal") }
    var selectedSectors by remember { mutableStateOf(recordToEdit?.sectors?.toSet() ?: setOf<String>()) }
    
    val today = FileViewModel.getCurrentDate()
    var dateReceivedGov by remember { mutableStateOf(recordToEdit?.dateReceivedGov ?: today) }
    var dateDeliveredToDomain by remember { mutableStateOf(recordToEdit?.dateDeliveredToDomain ?: today) }
    var dateRegistered by remember { mutableStateOf(recordToEdit?.dateRegistered ?: today) }

    var showReceivedPicker by remember { mutableStateOf(false) }
    var showDeliveredPicker by remember { mutableStateOf(false) }

    val receivedPickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val deliveredPickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    LaunchedEffect(receivedPickerState.selectedDateMillis) { receivedPickerState.selectedDateMillis?.let { dateReceivedGov = formatDate(it) } }
    LaunchedEffect(deliveredPickerState.selectedDateMillis) { deliveredPickerState.selectedDateMillis?.let { dateDeliveredToDomain = formatDate(it) } }

    if (showReceivedPicker) {
        DatePickerDialog(onDismissRequest = { showReceivedPicker = false }, confirmButton = { TextButton(onClick = { showReceivedPicker = false }) { Text("OK") } }) { DatePicker(state = receivedPickerState) }
    }
    if (showDeliveredPicker) {
        DatePickerDialog(onDismissRequest = { showDeliveredPicker = false }, confirmButton = { TextButton(onClick = { showDeliveredPicker = false }) { Text("OK") } }) { DatePicker(state = deliveredPickerState) }
    }

    var attachments by remember { mutableStateOf<List<Attachment>>(recordToEdit?.attachments ?: emptyList()) }
    var tempCameraAttachment by remember { mutableStateOf<Attachment?>(null) }
    var attachmentError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(attachmentError) { attachmentError?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long); attachmentError = null } }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val savedAttachments = mutableListOf<Attachment>()
        uris.forEach { uri ->
            try {
                val attachment = AttachmentStorage.copyToInternalStorage(context, uri)
                if (attachment != null) savedAttachments.add(attachment)
            } catch (e: Exception) { }
        }
        attachments = attachments + savedAttachments
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val savedAttachments = mutableListOf<Attachment>()
        uris.forEach { uri ->
            try {
                val attachment = AttachmentStorage.copyToInternalStorage(context, uri)
                if (attachment != null) savedAttachments.add(attachment)
            } catch (e: Exception) { }
        }
        attachments = attachments + savedAttachments
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraAttachment?.let { attachments = attachments + AttachmentStorage.updateAttachmentSize(it) }
        }
    }

    fun prepareCameraCapture(): Uri {
        val (file, attachment) = AttachmentStorage.createCameraAttachment(context)
        tempCameraAttachment = attachment
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val sectorMap = mapOf(
        "Educational Affairs" to R.string.sector_educational_affairs,
        "Planning" to R.string.sector_planning,
        "Orientation" to R.string.sector_orientation,
        "Buildings" to R.string.sector_buildings,
        "Mail Writing" to R.string.sector_mail_writing,
        "Finance" to R.string.sector_finance_main,
        "Information System" to R.string.sector_information_system,
        "Exams" to R.string.sector_exams,
        "Legal Affairs" to R.string.sector_legal_affairs,
        "HR Management" to R.string.sector_hr_management,
        "Inspection" to R.string.sector_inspection
    )
    val sectors = sectorMap.keys.toList()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val titleRes = if (recordToEdit != null) R.string.edit else R.string.new_file
                    Text(stringResource(titleRes), fontWeight = FontWeight.Bold) 
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(selected = selectedUrgency == "Normal", onClick = { selectedUrgency = "Normal" }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(stringResource(R.string.normal)) }
                SegmentedButton(selected = selectedUrgency == "Urgent", onClick = { selectedUrgency = "Urgent" }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(stringResource(R.string.urgent)) }
            }

            Text(stringResource(R.string.identification), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            
            Text(stringResource(R.string.original_serial), style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(expanded = yearDropdownExpanded, onExpandedChange = { yearDropdownExpanded = it }, modifier = Modifier.width(110.dp)) {
                    OutlinedTextField(value = "$selectedYear/", onValueChange = {}, readOnly = true, label = { Text("Year") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(yearDropdownExpanded) }, modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = yearDropdownExpanded, onDismissRequest = { yearDropdownExpanded = false }) { years.forEach { DropdownMenuItem(text = { Text("$it/") }, onClick = { selectedYear = it; yearDropdownExpanded = false }) } }
                }
                OutlinedTextField(value = originalSerialDigits, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) originalSerialDigits = it }, label = { Text("XXXX") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            }

            Text(stringResource(R.string.internal_serial), style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(expanded = internalYearDropdownExpanded, onExpandedChange = { internalYearDropdownExpanded = it }, modifier = Modifier.width(110.dp)) {
                    OutlinedTextField(value = "$internalYear/", onValueChange = {}, readOnly = true, label = { Text("Year") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(internalYearDropdownExpanded) }, modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = internalYearDropdownExpanded, onDismissRequest = { internalYearDropdownExpanded = false }) { years.forEach { DropdownMenuItem(text = { Text("$it/") }, onClick = { internalYear = it; internalYearDropdownExpanded = false }) } }
                }
                OutlinedTextField(value = internalSerialDigits, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) internalSerialDigits = it }, label = { Text("XXXX") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            }

            Text(stringResource(R.string.tracking_dates), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = dateReceivedGov, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.received_gov)) }, trailingIcon = { IconButton(onClick = { showReceivedPicker = true }) { Icon(Icons.Default.CalendarMonth, null) } }, modifier = Modifier.weight(1f).clickable { showReceivedPicker = true }, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = dateDeliveredToDomain, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.delivered_sector)) }, trailingIcon = { IconButton(onClick = { showDeliveredPicker = true }) { Icon(Icons.Default.CalendarMonth, null) } }, modifier = Modifier.weight(1f).clickable { showDeliveredPicker = true }, shape = RoundedCornerShape(12.dp))
            }
            OutlinedTextField(value = dateRegistered, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.registered_app)) }, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            OutlinedTextField(value = recipientName, onValueChange = { recipientName = it }, label = { Text(stringResource(R.string.recipient_name)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            
            // --- Source Dropdown ---
            Text(stringResource(R.string.source_label), style = MaterialTheme.typography.labelMedium)
            Box(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = sourceDropdownExpanded,
                    onExpandedChange = { sourceDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSourceText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.source_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sourceDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = sourceDropdownExpanded,
                        onDismissRequest = { sourceDropdownExpanded = false }
                    ) {
                        // 1-3. Hardcoded Defaults
                        defaultSources.forEach { resId ->
                            val name = stringResource(resId)
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedSourceText = name
                                    isOtherSelected = false
                                    sourceDropdownExpanded = false
                                }
                            )
                        }
                        
                        // 4. Custom Saved Sources (Room)
                        savedSources.forEach { saved ->
                            DropdownMenuItem(
                                text = { Text(saved.sourceName) },
                                onClick = {
                                    selectedSourceText = saved.sourceName
                                    isOtherSelected = false
                                    sourceDropdownExpanded = false
                                }
                            )
                        }
                        
                        // 5. Other (Always Last)
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.source_other)) },
                            onClick = {
                                selectedSourceText = context.getString(R.string.source_other)
                                isOtherSelected = true
                                sourceDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            if (isOtherSelected || (selectedSourceText == stringResource(R.string.source_other))) {
                OutlinedTextField(
                    value = customSourceText,
                    onValueChange = { customSourceText = it },
                    label = { Text(stringResource(R.string.custom_source_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = customSourceText.isEmpty() && (isOtherSelected)
                )
            }

            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text(stringResource(R.string.subject)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            
            Text(stringResource(R.string.destination_sectors), style = MaterialTheme.typography.titleSmall)
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sectors.forEach { sectorKey ->
                    FilterChip(
                        selected = sectorKey in selectedSectors,
                        onClick = {
                            selectedSectors = if (sectorKey in selectedSectors) {
                                selectedSectors - sectorKey
                            } else {
                                selectedSectors + sectorKey
                            }
                        },
                        label = { Text(stringResource(sectorMap[sectorKey]!!)) }
                    )
                }
            }

            Text(stringResource(R.string.attachments), style = MaterialTheme.typography.titleSmall)
            if (attachments.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(attachments) { attachment ->
                        AttachmentPreview(
                            attachment = attachment,
                            onClick = {
                                viewModel.openImageViewer(
                                    attachments.map { it.path },
                                    attachments.indexOf(attachment)
                                )
                                onOpenImageViewer()
                            },
                            onRemove = { attachments = attachments - attachment; AttachmentStorage.deleteAttachment(attachment.path) }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { try { val uri = prepareCameraCapture(); cameraLauncher.launch(uri) } catch (e: Exception) { } }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.CameraAlt, null); Text(" ${stringResource(R.string.camera)}") }
                Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Text(" Img") }
                Button(onClick = { fileLauncher.launch(arrayOf("application/pdf")) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Description, null); Text(" PDF") }
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(12.dp))
            
            val buttonText = if (recordToEdit != null) stringResource(R.string.update_button) else stringResource(R.string.register_button)
            Button(
                onClick = { 
                    val finalSource = if (isOtherSelected || selectedSourceText == context.getString(R.string.source_other)) {
                        customSourceText.trim()
                    } else {
                        selectedSourceText
                    }

                    if (finalSource.length < 2) {
                        Toast.makeText(context, context.getString(R.string.source_error), Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (originalSerialDigits.isNotBlank() && internalSerialDigits.isNotBlank()) {
                        val record = FileRecord(
                            id = recordToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                            originalSerial = "$selectedYear/$originalSerialDigits",
                            internalSerial = "$internalYear/$internalSerialDigits",
                            dateReceivedGov = dateReceivedGov,
                            dateRegistered = dateRegistered,
                            dateDeliveredToDomain = dateDeliveredToDomain,
                            recipientName = recipientName,
                            source = finalSource,
                            status = recordToEdit?.status ?: "Received",
                            attachments = attachments,
                            subject = subject,
                            urgency = selectedUrgency,
                            sectors = selectedSectors.toList(),
                            notes = notes
                        )
                        
                        // Save custom source to Room if it was typed or if it's a custom saved one (to update timestamp)
                        val isDefault = defaultSources.any { context.getString(it) == finalSource }
                        if (!isDefault) {
                            viewModel.handleSourceSavingToRoom(finalSource)
                        }

                        if (recordToEdit != null) {
                            viewModel.updateRecord(record)
                            Toast.makeText(context, context.getString(R.string.edit_success), Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addRecord(record)
                        }
                        onNavigateBack()
                    }
                }, 
                modifier = Modifier.fillMaxWidth().height(56.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedUrgency == "Urgent") StatusUrgent else MaterialTheme.colorScheme.primary)
            ) { 
                Text(buttonText, fontWeight = FontWeight.Bold) 
            }
        }
    }
}

@Composable
fun AttachmentPreview(attachment: Attachment, onClick: () -> Unit, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).clickable { onClick() }) {
        if (attachment.type == "application/pdf") {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                    Text("PDF", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            AsyncImage(
                model = File(attachment.path),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
