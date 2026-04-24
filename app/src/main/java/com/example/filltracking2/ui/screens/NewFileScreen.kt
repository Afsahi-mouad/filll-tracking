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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
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
    onNavigateBack: () -> Unit
) {
    val years = listOf("25", "26", "27")
    var selectedYear by remember { mutableStateOf("26") }
    var yearDropdownExpanded by remember { mutableStateOf(false) }
    var originalSerialDigits by remember { mutableStateOf("") }

    var internalYear by remember { mutableStateOf("26") }
    var internalYearDropdownExpanded by remember { mutableStateOf(false) }
    var internalSerialDigits by remember { mutableStateOf("") }

    var recipientName by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedUrgency by remember { mutableStateOf("Normal") }
    var selectedSectors by remember { mutableStateOf(setOf<String>()) }
    
    val today = FileViewModel.getCurrentDate()
    var dateReceivedGov by remember { mutableStateOf(today) }
    var dateDeliveredToDomain by remember { mutableStateOf(today) }

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

    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var tempCameraAttachment by remember { mutableStateOf<Attachment?>(null) }
    var attachmentError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
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

    val sectors = listOf("Finance", "Technical", "HR", "Security", "Admin", "Legal")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ThemeManager.getString("new_file"), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(selected = selectedUrgency == "Normal", onClick = { selectedUrgency = "Normal" }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(ThemeManager.getString("normal")) }
                SegmentedButton(selected = selectedUrgency == "Urgent", onClick = { selectedUrgency = "Urgent" }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(ThemeManager.getString("urgent")) }
            }

            Text(ThemeManager.getString("identification"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            
            Text(ThemeManager.getString("original_serial"), style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(expanded = yearDropdownExpanded, onExpandedChange = { yearDropdownExpanded = it }, modifier = Modifier.width(110.dp)) {
                    OutlinedTextField(value = "$selectedYear/", onValueChange = {}, readOnly = true, label = { Text("Year") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(yearDropdownExpanded) }, modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = yearDropdownExpanded, onDismissRequest = { yearDropdownExpanded = false }) { years.forEach { DropdownMenuItem(text = { Text("$it/") }, onClick = { selectedYear = it; yearDropdownExpanded = false }) } }
                }
                OutlinedTextField(value = originalSerialDigits, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) originalSerialDigits = it }, label = { Text("XXXX") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            }

            Text(ThemeManager.getString("internal_serial"), style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(expanded = internalYearDropdownExpanded, onExpandedChange = { internalYearDropdownExpanded = it }, modifier = Modifier.width(110.dp)) {
                    OutlinedTextField(value = "$internalYear/", onValueChange = {}, readOnly = true, label = { Text("Year") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(internalYearDropdownExpanded) }, modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = internalYearDropdownExpanded, onDismissRequest = { internalYearDropdownExpanded = false }) { years.forEach { DropdownMenuItem(text = { Text("$it/") }, onClick = { internalYear = it; internalYearDropdownExpanded = false }) } }
                }
                OutlinedTextField(value = internalSerialDigits, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) internalSerialDigits = it }, label = { Text("XXXX") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            }

            Text(ThemeManager.getString("tracking_dates"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = dateReceivedGov, onValueChange = {}, readOnly = true, label = { Text(ThemeManager.getString("received_gov")) }, trailingIcon = { IconButton(onClick = { showReceivedPicker = true }) { Icon(Icons.Default.CalendarMonth, null) } }, modifier = Modifier.weight(1f).clickable { showReceivedPicker = true }, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = dateDeliveredToDomain, onValueChange = {}, readOnly = true, label = { Text(ThemeManager.getString("delivered_sector")) }, trailingIcon = { IconButton(onClick = { showDeliveredPicker = true }) { Icon(Icons.Default.CalendarMonth, null) } }, modifier = Modifier.weight(1f).clickable { showDeliveredPicker = true }, shape = RoundedCornerShape(12.dp))
            }
            OutlinedTextField(value = FileViewModel.getCurrentDate(), onValueChange = {}, readOnly = true, label = { Text(ThemeManager.getString("registered_app")) }, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            OutlinedTextField(value = recipientName, onValueChange = { recipientName = it }, label = { Text(ThemeManager.getString("recipient_name")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text(ThemeManager.getString("subject")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            
            Text(ThemeManager.getString("destination_sectors"), style = MaterialTheme.typography.titleSmall)
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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

            Text(ThemeManager.getString("attachments"), style = MaterialTheme.typography.titleSmall)
            if (attachments.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(attachments) { attachment ->
                        AttachmentPreview(attachment = attachment, onRemove = { attachments = attachments - attachment; AttachmentStorage.deleteAttachment(attachment.path) })
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { try { val uri = prepareCameraCapture(); cameraLauncher.launch(uri) } catch (e: Exception) { } }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.CameraAlt, null); Text(" ${ThemeManager.getString("camera")}") }
                Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Text(" ${ThemeManager.getString("gallery")}") }
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(ThemeManager.getString("notes")) }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(12.dp))
            
            Button(onClick = { if (originalSerialDigits.isNotBlank() && internalSerialDigits.isNotBlank()) { viewModel.addRecord(FileRecord(originalSerial = "$selectedYear/$originalSerialDigits", internalSerial = "$internalYear/$internalSerialDigits", dateReceivedGov = dateReceivedGov, dateRegistered = FileViewModel.getCurrentDate(), dateDeliveredToDomain = dateDeliveredToDomain, recipientName = recipientName, status = "Received", attachments = attachments, subject = subject, urgency = selectedUrgency, sectors = selectedSectors.toList(), notes = notes)); onNavigateBack() } }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selectedUrgency == "Urgent") StatusUrgent else MaterialTheme.colorScheme.primary)) { Text(ThemeManager.getString("register_button"), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun AttachmentPreview(attachment: Attachment, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(80.dp)) {
        AsyncImage(
            model = File(attachment.path),
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
