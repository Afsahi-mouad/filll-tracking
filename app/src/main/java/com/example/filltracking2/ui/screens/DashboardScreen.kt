package com.example.filltracking2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.ui.theme.StatusPending
import com.example.filltracking2.ui.theme.StatusProcessed
import com.example.filltracking2.ui.theme.StatusReceived
import com.example.filltracking2.ui.theme.StatusUrgent
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
    var currentView by remember { mutableStateOf("Director") } // "Director" or "Sector view"
    
    val records by viewModel.records.collectAsState()
    
    var selectedYear by remember { mutableStateOf("26") }
    var yearDropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    
    val allSectors = listOf("Finance", "Legal", "HR", "Operations", "Technical", "Security", "Admin")
    var selectedSectorTab by remember { mutableStateOf(allSectors[2]) } // Default to HR
    
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                Text(
                    "FILE TRACKER",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NavigationDrawerItem(
                    label = { Text("Director") },
                    selected = currentView == "Director",
                    onClick = {
                        currentView = "Director"
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Circle, null, modifier = Modifier.size(8.dp)) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Sector view") },
                    selected = currentView == "Sector view",
                    onClick = {
                        currentView = "Sector view"
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Circle, null, modifier = Modifier.size(8.dp), tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            if (currentView == "Sector view") "Sector View" else "Fill Tracking",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    },
                    actions = {
                        BadgedBox(
                            badge = {
                                Badge { Text("3") }
                            }
                        ) {
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.Notifications, "Notifications")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (currentView == "Sector view") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "Sector Dashboard",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "VIEWING AS SECTOR:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    ScrollableTabRow(
                        selectedTabIndex = allSectors.indexOf(selectedSectorTab).coerceAtLeast(0),
                        edgePadding = 16.dp,
                        containerColor = Color.Transparent,
                        divider = { HorizontalDivider() },
                        indicator = { tabPositions ->
                            val index = allSectors.indexOf(selectedSectorTab)
                            if (index != -1) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[index]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        allSectors.forEach { sector ->
                            Tab(
                                selected = selectedSectorTab == sector,
                                onClick = { selectedSectorTab = sector },
                                text = { 
                                    Text(
                                        sector,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selectedSectorTab == sector) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (currentView == "Director") {
                        // Search Bar with Year Filter
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Year Selector
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
                                            Text(
                                                "20$selectedYear",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = yearDropdownExpanded,
                                        onDismissRequest = { yearDropdownExpanded = false }
                                    ) {
                                        listOf("24", "25", "26", "27").forEach { year ->
                                            DropdownMenuItem(
                                                text = { Text("Year 20$year") },
                                                onClick = {
                                                    selectedYear = year
                                                    yearDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Search Field
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { if (it.length <= 4) searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    prefix = { 
                                        Text(
                                            "$selectedYear/",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        ) 
                                    },
                                    placeholder = { Text("0000") },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, "Clear")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                            }
                        }
                        
                        // Stats Cards
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                item {
                                    StatCard(
                                        title = "Total Files",
                                        count = stats.total,
                                        icon = Icons.Outlined.CheckCircle,
                                        color = MaterialTheme.colorScheme.primary,
                                        isSelected = selectedFilter == "All",
                                        onClick = { selectedFilter = "All" }
                                    )
                                }
                                item {
                                    StatCard(
                                        title = "Urgent",
                                        count = stats.urgent,
                                        icon = Icons.Outlined.ErrorOutline,
                                        color = StatusUrgent,
                                        isSelected = selectedFilter == "Urgent",
                                        onClick = { selectedFilter = "Urgent" }
                                    )
                                }
                                item {
                                    StatCard(
                                        title = "Processed",
                                        count = stats.processed,
                                        icon = Icons.Outlined.CheckCircle,
                                        color = StatusProcessed,
                                        isSelected = selectedFilter == "Processed",
                                        onClick = { selectedFilter = "Processed" }
                                    )
                                }
                                item {
                                    StatCard(
                                        title = "Pending",
                                        count = stats.pending,
                                        icon = Icons.Outlined.Schedule,
                                        color = StatusPending,
                                        isSelected = selectedFilter == "Pending",
                                        onClick = { selectedFilter = "Pending" }
                                    )
                                }
                            }
                        }
                        
                        // Filter Chips
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val filters = listOf("All", "Urgent", "Pending", "Received", "By Receiver")
                                items(filters) { filter ->
                                    FilterChip(
                                        selected = selectedFilter == filter,
                                        onClick = { selectedFilter = filter },
                                        label = { Text(filter) },
                                        leadingIcon = if (selectedFilter == filter) {
                                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                        
                        // Recent Files Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Recent Files",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { }) {
                                    Text("View All")
                                }
                            }
                        }
                    }
                    
                    // File List
                    items(filteredRecords, key = { it.id }) { record ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically()
                        ) {
                            FileCard(
                                record = record,
                                onClick = { onFileClick(record) }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class DashboardStats(
    val total: Int,
    val urgent: Int,
    val processed: Int,
    val pending: Int
)

@Composable
fun StatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.15f) 
                           else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, color.copy(alpha = 0.5f))
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
                if (count > 0) {
                    Badge(containerColor = color) {
                        Text(count.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FileCard(
    record: FileRecord,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Serial + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Indicator
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (record.status) {
                                    "Received" -> StatusReceived
                                    "Pending" -> StatusPending
                                    "Processed" -> StatusProcessed
                                    else -> Color.Gray
                                }
                            )
                    )
                    Text(
                        "Original: ${record.originalSerial}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Pill
                StatusPill(status = record.status, urgency = record.urgency)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Subject
            Text(
                record.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Recipient & Date
            Text(
                "${record.recipientName} • ${record.dateReceivedGov}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bottom Row: Sectors + Attachment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sector Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    record.sectors.take(2).forEach { sector ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                sector,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                // Attachment indicator
                if (record.attachments.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AttachFile,
                            "Has attachment",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        if (record.attachments.size > 1) {
                            Text(
                                " +${record.attachments.size - 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: String, urgency: String) {
    val (backgroundColor, textColor, icon) = when {
        urgency == "Urgent" -> Triple(
            StatusUrgent.copy(alpha = 0.15f),
            StatusUrgent,
            Icons.Outlined.ErrorOutline
        )
        status == "Processed" -> Triple(
            StatusProcessed.copy(alpha = 0.15f),
            StatusProcessed,
            Icons.Outlined.CheckCircle
        )
        status == "Pending" -> Triple(
            StatusPending.copy(alpha = 0.15f),
            StatusPending,
            Icons.Outlined.Schedule
        )
        else -> Triple(
            StatusReceived.copy(alpha = 0.15f),
            StatusReceived,
            Icons.Outlined.Pending
        )
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(14.dp))
            Text(
                if (urgency == "Urgent") "URGENT" else status.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
