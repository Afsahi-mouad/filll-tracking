package com.example.filltracking2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.filltracking2.util.LocaleManager
import com.example.filltracking2.Screen
import com.example.filltracking2.R
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.ui.theme.*
import com.example.filltracking2.ui.viewmodel.FileViewModel
import kotlinx.coroutines.launch

private val sectorMap = mapOf(
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
    "Inspection" to R.string.sector_inspection,
    "Security" to R.string.sector_security,
    "Admin" to R.string.sector_admin,
    "Operations" to R.string.sector_operations,
    "General" to R.string.sector_general,
    "Technical" to R.string.sector_technical
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: FileViewModel,
    onOpenDrawer: () -> Unit,
    onFileClick: (FileRecord) -> Unit,
    initialView: String = "Director",
    initialSector: String? = null
) {
    // currentView is no longer strictly needed if we navigate away for Sector View,
    // but we'll keep it for internal state if someone clicks the drawer item.
    // However, navigation is better for deep linking.
    var currentView by remember { mutableStateOf(initialView) }
    
    val records by viewModel.records.collectAsState()
    
    var selectedYear by remember { mutableStateOf("26") }
    var yearDropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    
    val allSectors = sectorMap.keys.toList()
    var selectedSectorTab by remember { mutableStateOf(initialSector ?: allSectors[0]) } 
    
    val filteredRecords = remember(records, searchQuery, selectedFilter, currentView, selectedSectorTab, selectedYear) {
        records.filter { record ->
            val matchesSearch = searchQuery.isEmpty() ||
                record.internalSerial.contains("$selectedYear/$searchQuery", ignoreCase = true) ||
                record.originalSerial.contains(searchQuery, ignoreCase = true) ||
                record.recipientName.contains(searchQuery, ignoreCase = true) ||
                record.subject.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = if (currentView == "Sector view") {
                record.sectors.contains(selectedSectorTab)
            } else {
                when (selectedFilter) {
                    "Urgent" -> record.urgency.equals("Urgent", ignoreCase = true)
                    "Normal" -> !record.urgency.equals("Urgent", ignoreCase = true)
                    "Received" -> record.status.equals("Received", ignoreCase = true)
                    else -> true
                }
            }
            matchesSearch && matchesFilter
        }
    }

    val stats = remember(records) {
        DashboardStats(
            total = records.size,
            urgent = records.count { it.urgency.equals("Urgent", ignoreCase = true) },
            normal = records.count { !it.urgency.equals("Urgent", ignoreCase = true) }
        )
    }

    val todayCount = remember(records) {
        val today = FileViewModel.getCurrentDate()
        records.count { it.dateRegistered == today }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val title = if (currentView == "Sector view") stringResource(R.string.sector_dashboard) else stringResource(R.string.home)
                    Text(title, fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (todayCount > 0) {
                                Badge { Text("$todayCount") }
                            }
                        }
                    ) {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Notifications, "Notifications")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentView == "Director") {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box {
                                Surface(
                                    onClick = { yearDropdownExpanded = true },
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("20$selectedYear", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(expanded = yearDropdownExpanded, onDismissRequest = { yearDropdownExpanded = false }) {
                                    listOf("24", "25", "26", "27").forEach { year ->
                                        DropdownMenuItem(
                                            text = { Text("20$year") },
                                            onClick = { selectedYear = year; yearDropdownExpanded = false }
                                        )
                                    }
                                }
                            }
                            
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(stringResource(R.string.search_hint)) },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val cardModifier = Modifier.weight(1f)
                            StatCard(stringResource(R.string.total), stats.total, Icons.Default.Description, MoroccoPrimary, selectedFilter == "All", cardModifier) { selectedFilter = "All" }
                            StatCard(stringResource(R.string.urgent), stats.urgent, Icons.Default.PriorityHigh, StatusUrgent, selectedFilter == "Urgent", cardModifier) { selectedFilter = "Urgent" }
                            StatCard(stringResource(R.string.normal), stats.normal, Icons.Default.CheckCircle, StatusProcessed, selectedFilter == "Normal", cardModifier) { selectedFilter = "Normal" }
                        }
                    }

                    item {
                        Text(
                            stringResource(R.string.recent_files),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    item {
                        ScrollableTabRow(
                            selectedTabIndex = allSectors.indexOf(selectedSectorTab),
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent,
                            divider = {},
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[allSectors.indexOf(selectedSectorTab)]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            allSectors.forEach { sector ->
                                val resId = sectorMap[sector]
                                Tab(
                                    selected = selectedSectorTab == sector,
                                    onClick = { selectedSectorTab = sector },
                                    text = { Text(if (resId != null) stringResource(resId) else sector, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }
                items(filteredRecords, key = { it.id }) { record ->
                    FileCard(record = record, onClick = { onFileClick(record) })
                }
            }
        }
    }
}

data class DashboardStats(val total: Int, val urgent: Int, val normal: Int)

@Composable
fun StatCard(title: String, count: Int, icon: ImageVector, color: Color, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.15f) else if (ThemeManager.isDarkTheme) DarkSurface else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, color.copy(alpha = 0.5f)) 
                 else BorderStroke(1.dp, if (ThemeManager.isDarkTheme) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
                if (count >= 0) {
                    Surface(
                        color = color,
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                count.toString(), 
                                style = MaterialTheme.typography.labelSmall, 
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Text(
                title, 
                style = MaterialTheme.typography.labelMedium, 
                fontWeight = FontWeight.Bold,
                color = if (ThemeManager.isDarkTheme) Color.White else Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FileCard(record: FileRecord, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (record.status) {
                        "Processed" -> StatusProcessed
                        else -> StatusReceived
                    }
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                    Text("${stringResource(R.string.original_serial_label)}: ${record.originalSerial}", style = MaterialTheme.typography.labelSmall)
                }
                StatusPill(status = record.status, urgency = record.urgency)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(record.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${record.recipientName} • ${record.dateReceivedGov}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    record.sectors.take(2).forEach { sectorKey ->
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                            val resId = sectorMap[sectorKey]
                            val label = if (resId != null) stringResource(resId) else sectorKey
                            Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (record.attachments.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        if (record.attachments.size > 1) Text(" +${record.attachments.size - 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: String, urgency: String) {
    val displayStatus = if (status == "Pending") "Received" else status
    val (bgColor, txtColor, icon) = when {
        urgency == "Urgent" -> Triple(StatusUrgent.copy(alpha = 0.15f), StatusUrgent, Icons.Outlined.ErrorOutline)
        displayStatus == "Processed" -> Triple(StatusProcessed.copy(alpha = 0.15f), StatusProcessed, Icons.Outlined.CheckCircle)
        else -> Triple(StatusReceived.copy(alpha = 0.15f), StatusReceived, Icons.Outlined.Inbox)
    }
    Surface(color = bgColor, shape = RoundedCornerShape(20.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = txtColor, modifier = Modifier.size(14.dp))
            Text(if (urgency == "Urgent") "URGENT" else displayStatus.uppercase(), style = MaterialTheme.typography.labelSmall, color = txtColor, fontWeight = FontWeight.Bold)
        }
    }
}
