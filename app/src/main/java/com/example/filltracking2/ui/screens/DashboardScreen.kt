package com.example.filltracking2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.ui.theme.*
import com.example.filltracking2.ui.viewmodel.FileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FileViewModel,
    onFileClick: (FileRecord) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentView by remember { mutableStateOf("Director") }
    
    val records by viewModel.records.collectAsState()
    
    var selectedYear by remember { mutableStateOf("26") }
    var yearDropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    
    val allSectors = listOf("Finance", "Legal", "HR", "Operations", "Technical", "Security", "Admin")
    var selectedSectorTab by remember { mutableStateOf(allSectors[0]) } 
    
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
                    "Urgent" -> record.urgency == "Urgent"
                    "Pending" -> record.status == "Pending"
                    "Received" -> record.status == "Received"
                    "Processed" -> record.status == "Processed"
                    else -> true
                }
            }
            matchesSearch && matchesFilter
        }
    }

    val stats = remember(records) {
        DashboardStats(
            total = records.size,
            urgent = records.count { it.urgency == "Urgent" },
            processed = records.count { it.status == "Processed" },
            pending = records.count { it.status == "Pending" }
        )
    }

    val todayCount = remember(records) {
        val today = FileViewModel.getCurrentDate()
        records.count { it.dateRegistered == today }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Text("FILE TRACKER", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelMedium)
                NavigationDrawerItem(
                    label = { Text("Director (Home)") },
                    selected = currentView == "Director",
                    onClick = { currentView = "Director"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Dashboard, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Sector View") },
                    selected = currentView == "Sector view",
                    onClick = { currentView = "Sector view"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Business, null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (currentView == "Sector view") "Sector Dashboard" else ThemeManager.getString("home"), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
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
                if (currentView == "Sector view") {
                    ScrollableTabRow(
                        selectedTabIndex = allSectors.indexOf(selectedSectorTab).coerceAtLeast(0),
                        edgePadding = 16.dp,
                        containerColor = Color.Transparent,
                        indicator = { tabPositions ->
                            val index = allSectors.indexOf(selectedSectorTab)
                            if (index != -1) {
                                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[index]))
                            }
                        }
                    ) {
                        allSectors.forEach { sector ->
                            Tab(
                                selected = selectedSectorTab == sector,
                                onClick = { selectedSectorTab = sector },
                                text = { Text(sector) }
                            )
                        }
                    }
                }

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
                                            DropdownMenuItem(text = { Text("Year 20$year") }, onClick = { selectedYear = year; yearDropdownExpanded = false })
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { if (it.length <= 4) searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    prefix = { Text("$selectedYear/", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                    placeholder = { Text("0000") },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                                item { StatCard("Total", stats.total, Icons.Outlined.CheckCircle, MaterialTheme.colorScheme.primary, selectedFilter == "All") { selectedFilter = "All" } }
                                item { StatCard("Urgent", stats.urgent, Icons.Outlined.ErrorOutline, StatusUrgent, selectedFilter == "Urgent") { selectedFilter = "Urgent" } }
                                item { StatCard("Processed", stats.processed, Icons.Outlined.CheckCircle, StatusProcessed, selectedFilter == "Processed") { selectedFilter = "Processed" } }
                                item { StatCard("Pending", stats.pending, Icons.Outlined.Schedule, StatusPending, selectedFilter == "Pending") { selectedFilter = "Pending" } }
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
}

data class DashboardStats(val total: Int, val urgent: Int, val processed: Int, val pending: Int)

@Composable
fun StatCard(title: String, count: Int, icon: ImageVector, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(140.dp).height(100.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, color.copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
                if (count > 0) Badge(containerColor = color) { Text(count.toString()) }
            }
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun FileCard(record: FileRecord, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(when (record.status) { "Received" -> StatusReceived; "Pending" -> StatusPending; "Processed" -> StatusProcessed; else -> Color.Gray }))
                    Text("Original: ${record.originalSerial}", style = MaterialTheme.typography.labelMedium)
                }
                StatusPill(status = record.status, urgency = record.urgency)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(record.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${record.recipientName} • ${record.dateReceivedGov}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    record.sectors.take(2).forEach { sector ->
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(sector, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
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
    val (bgColor, txtColor, icon) = when {
        urgency == "Urgent" -> Triple(StatusUrgent.copy(alpha = 0.15f), StatusUrgent, Icons.Outlined.ErrorOutline)
        status == "Processed" -> Triple(StatusProcessed.copy(alpha = 0.15f), StatusProcessed, Icons.Outlined.CheckCircle)
        status == "Pending" -> Triple(StatusPending.copy(alpha = 0.15f), StatusPending, Icons.Outlined.Schedule)
        else -> Triple(StatusReceived.copy(alpha = 0.15f), StatusReceived, Icons.Outlined.Pending)
    }
    Surface(color = bgColor, shape = RoundedCornerShape(20.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = txtColor, modifier = Modifier.size(14.dp))
            Text(if (urgency == "Urgent") "URGENT" else status.uppercase(), style = MaterialTheme.typography.labelSmall, color = txtColor, fontWeight = FontWeight.Bold)
        }
    }
}
