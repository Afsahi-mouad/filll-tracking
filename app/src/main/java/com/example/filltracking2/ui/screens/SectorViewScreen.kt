package com.example.filltracking2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.filltracking2.R
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.ui.theme.*
import com.example.filltracking2.ui.viewmodel.FileViewModel

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
fun SectorViewScreen(
    viewModel: FileViewModel,
    filterBySector: String = "",
    onOpenDrawer: () -> Unit,
    onFileClick: (FileRecord) -> Unit,
    onNavigateBack: () -> Unit
) {
    val records by viewModel.records.collectAsState()
    val allSectors = sectorMap.keys.toList()
    
    var selectedSectorTab by remember { 
        mutableStateOf(if (filterBySector.isNotEmpty() && allSectors.contains(filterBySector)) filterBySector else allSectors[0]) 
    }

    val filteredRecords = remember(records, selectedSectorTab) {
        records.filter { it.sectors.contains(selectedSectorTab) }
    }

    Scaffold(
        containerColor = if (ThemeManager.isDarkTheme) Color.Black else Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (ThemeManager.isDarkTheme) Color.Black else Color(0xFFF5F5F5)
                ),
                title = { Text(stringResource(R.string.sector_dashboard), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (filterBySector.isEmpty()) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = allSectors.indexOf(selectedSectorTab).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    val index = allSectors.indexOf(selectedSectorTab)
                    if (index != -1) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[index]),
                            color = if (ThemeManager.isDarkTheme) AccentGold else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                allSectors.forEach { sectorKey ->
                    Tab(
                        selected = selectedSectorTab == sectorKey,
                        onClick = { selectedSectorTab = sectorKey },
                        text = { 
                            Text(
                                stringResource(sectorMap[sectorKey]!!),
                                color = if (selectedSectorTab == sectorKey) {
                                    if (ThemeManager.isDarkTheme) AccentGold else MaterialTheme.colorScheme.primary
                                } else {
                                    if (ThemeManager.isDarkTheme) Color.Gray else Color.DarkGray
                                },
                                fontWeight = if (selectedSectorTab == sectorKey) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRecords, key = { it.id }) { record ->
                    SectorFileCard(record = record, onClick = { onFileClick(record) })
                }
            }
        }
    }
}

@Composable
fun SectorFileCard(record: FileRecord, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (ThemeManager.isDarkTheme) AccentGold.copy(alpha = 0.2f) else MoroccoPrimary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (ThemeManager.isDarkTheme) DarkSurface else Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (record.status) {
                        "Processed" -> StatusProcessed
                        "Urgent" -> StatusUrgent
                        else -> StatusReceived
                    }
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                    Text(
                        "${stringResource(R.string.original_serial_label)}: ${record.originalSerial}", 
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ThemeManager.isDarkTheme) Color.White else Color.Black
                    )
                }
                StatusPill(status = record.status, urgency = record.urgency)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                record.subject, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold, 
                maxLines = 2, 
                overflow = TextOverflow.Ellipsis,
                color = if (ThemeManager.isDarkTheme) Color.White else Color.Black
            )
            Text(
                "${record.recipientName} • ${record.dateReceivedGov}", 
                style = MaterialTheme.typography.bodyMedium, 
                color = if (ThemeManager.isDarkTheme) Color.LightGray else Color.DarkGray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    record.sectors.take(1).forEach { sectorKey ->
                        Surface(
                            color = if (ThemeManager.isDarkTheme) MoroccoPrimary.copy(alpha = 0.3f) else MoroccoPrimary.copy(alpha = 0.1f), 
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val resId = sectorMap[sectorKey]
                            val label = if (resId != null) stringResource(resId) else sectorKey
                            Text(
                                label, 
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), 
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ThemeManager.isDarkTheme) AccentGold else MoroccoPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (record.attachments.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AttachFile, 
                            null, 
                            tint = if (ThemeManager.isDarkTheme) AccentGold else MoroccoPrimary, 
                            modifier = Modifier.size(20.dp)
                        )
                        if (record.attachments.size > 1) {
                            Text(
                                " +${record.attachments.size - 1}", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = if (ThemeManager.isDarkTheme) AccentGold else MoroccoPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
