package com.example.filltracking2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filltracking2.R
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.ui.theme.StatusProcessed
import com.example.filltracking2.ui.theme.StatusUrgent
import com.example.filltracking2.ui.theme.ThemeManager
import com.example.filltracking2.ui.viewmodel.FileViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: FileViewModel,
    onFileClick: (FileRecord) -> Unit
) {
    val allRecords by viewModel.records.collectAsState()

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(12) }
    val daysOfWeek = remember { daysOfWeek() }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )
    
    val coroutineScope = rememberCoroutineScope()
    val visibleMonth = state.firstVisibleMonth.yearMonth
    
    val recordsByDate = remember(allRecords) {
        val formatterPadded = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
        val formatterShort = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
        allRecords.groupBy { 
            try {
                LocalDate.parse(it.dateRegistered, formatterPadded)
            } catch (e: Exception) {
                try {
                    LocalDate.parse(it.dateRegistered, formatterShort)
                } catch (e2: Exception) {
                    null
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history), fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CalendarHeader(
                currentMonth = visibleMonth,
                onPreviousMonth = {
                    coroutineScope.launch { state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.previousMonth) }
                },
                onNextMonth = {
                    coroutineScope.launch { state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.nextMonth) }
                }
            )

            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    val dayRecords = recordsByDate[day.date] ?: emptyList()
                    Day(
                        day = day,
                        isSelected = selectedDate == day.date,
                        records = dayRecords,
                        onClick = { selectedDate = it.date }
                    )
                },
                monthHeader = { DaysOfWeekTitle(daysOfWeek = daysOfWeek) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

            val selectedDateRecords = recordsByDate[selectedDate] ?: emptyList()
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = selectedDate?.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd")) ?: "Select a date",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (selectedDateRecords.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.doc_not_found))
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(selectedDateRecords) { record ->
                            HistoryFileCard(record = record, onClick = { onFileClick(record) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Day(day: CalendarDay, isSelected: Boolean, records: List<FileRecord>, onClick: (CalendarDay) -> Unit) {
    val isCurrentMonth = day.position == DayPosition.MonthDate
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(enabled = isCurrentMonth) { onClick(day) },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = when {
                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (isCurrentMonth && records.isNotEmpty()) {
                if (records.size <= 3) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        records.forEach { record ->
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (record.urgency == "Urgent") StatusUrgent 
                                        else StatusProcessed
                                    )
                            )
                        }
                    }
                } else {
                    val urgentCount = records.count { it.urgency == "Urgent" }
                    val dominantColor = if (urgentCount > 0) StatusUrgent 
                                        else StatusProcessed
                    Text(
                        text = "${records.size}+",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else dominantColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DaysOfWeekTitle(daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CalendarHeader(currentMonth: YearMonth, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row {
            IconButton(onClick = onPreviousMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous") }
            IconButton(onClick = onNextMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next") }
        }
    }
}

@Composable
fun HistoryFileCard(record: FileRecord, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (record.urgency == "Urgent") 
                StatusUrgent.copy(alpha = 0.05f) 
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (record.urgency == "Urgent") 
            androidx.compose.foundation.BorderStroke(1.dp, StatusUrgent.copy(alpha = 0.2f)) 
        else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.subject, 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1,
                    color = if (record.urgency == "Urgent") StatusUrgent else Color.Unspecified
                )
                Text(text = "Serial: ${record.internalSerial} • ${record.recipientName}", style = MaterialTheme.typography.labelMedium)
            }
            
            if (record.urgency == "Urgent") {
                StatusPill(status = record.status, urgency = record.urgency)
            }
        }
    }
}
