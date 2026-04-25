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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    "Inspection" to R.string.sector_inspection
)

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
    
    val allSectors = sectorMap.keys.toList()
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Text(stringResource(R.string.file_tracker), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelMedium)
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.view_director)) },
                    selected = currentView == "Director",
                    onClick = { currentView = "Director"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Dashboard, null) }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.view_sector)) },
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
                    title = { 
                        val title = if (currentView == "Sector view") stringResource(R.string.sector_dashboard) else stringResource(R.string.home)
                        Text(title, fontWeight = FontWeight.Bold) 
                    },
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
                        allSectors.forEach { sectorKey ->
                            Tab(
                                selected = selectedSectorTab == sectorKey,
                                onClick = { selectedSectorTab = sectorKey },
                                text = { Text(stringResource(sectorMap[sectorKey]!!)) }
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
                                item { 
                                    StatCard(
                                        title = stringResource(R.string.total), 
                                        count = stats.total, 
                                        icon = Icons.Outlined.CheckCircle, 
                                        color = MaterialTheme.colorScheme.primary, 
                                        isSelected = selectedFilter == "All",
                                        onClick = { selectedFilter = "All" }
                                    ) 
                                }
                                item { 
                                    StatCard(
                                        title = stringResource(R.string.urgent), 
                                        count = stats.urgent, 
                                        icon = Icons.Outlined.ErrorOutline, 
                                        color = StatusUrgent, 
                                        isSelected = selectedFilter == "Urgent",
                                        onClick = { selectedFilter = "Urgent" }
                                    ) 
                                }
                                item { 
                                    StatCard(
                                        title = stringResource(R.string.normal), 
                                        count = stats.normal, 
                                        icon = Icons.Outlined.CheckCircle, 
                                        color = StatusProcessed, 
                                        isSelected = selectedFilter == "Normal",
                                        onClick = { selectedFilter = "Normal" }
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
}

data class DashboardStats(val total: Int, val urgent: Int, val normal: Int)

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
                    val statusColor = when (record.status) {
                        "Processed" -> StatusProcessed
                        else -> StatusReceived
                    }
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                    Text("${stringResource(R.string.original_serial_label)}: ${record.originalSerial}", style = MaterialTheme.typography.labelMedium)
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
