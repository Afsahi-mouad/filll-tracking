package com.example.filltracking2.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.example.filltracking2.ui.theme.FillTrackingTheme
import com.example.filltracking2.R
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.ui.viewmodel.FileViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RedUrgent = Color(0xFFE24B4A)

// Chart Palette
private val ChartPalette = listOf(
    Color(0xFFFFD700), // Gold
    Color(0xFF6C63FF), // Purple
    Color(0xFFFFC107), // Amber
    Color(0xFFE24B4A), // Red
    Color(0xFF378ADD)  // Blue
)

private val sectorIcons = mapOf(
    "Educational Affairs" to Icons.Default.School,
    "Planning" to Icons.Default.PieChart,
    "Orientation" to Icons.Default.Explore,
    "Buildings" to Icons.Default.Business,
    "Mail Writing" to Icons.Default.Edit,
    "Finance" to Icons.Default.AccountBalanceWallet,
    "Information System" to Icons.Default.Computer,
    "Exams" to Icons.AutoMirrored.Filled.Assignment,
    "Legal Affairs" to Icons.Default.Gavel,
    "HR Management" to Icons.Default.Groups,
    "Inspection" to Icons.Default.Search,
    "Security" to Icons.Default.Lock,
    "Admin" to Icons.Default.LocationCity,
    "Operations" to Icons.Default.Settings,
    "General" to Icons.Default.Assignment,
    "Technical" to Icons.Default.Build
)

private val sectorNames = mapOf(
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
fun AnalyticsScreen(
    viewModel: FileViewModel,
    onOpenDrawer: () -> Unit,
    onSectorClick: (String) -> Unit = {}
) {
    val records by viewModel.records.collectAsState()

    val urgentCount = remember(records) {
        records.count { it.urgency.equals("Urgent", ignoreCase = true) }
    }
    val normalCount = remember(records) {
        records.count { !it.urgency.equals("Urgent", ignoreCase = true) }
    }
    val totalCount = records.size

    val sectorCounts = remember(records) {
        val counts = mutableMapOf<String, Int>()
        records.forEach { record ->
            record.sectors.forEach { sector ->
                counts[sector] = counts.getOrDefault(sector, 0) + 1
            }
        }
        counts.filter { it.value > 0 }.toList().sortedByDescending { it.second }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analytics), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PriorityDistributionCard(urgentCount, normalCount, totalCount)
            SectorOverviewCard(sectorCounts, onSectorClick)
            StatusBreakdownCard(records)
            CumulativeCompletionsCard(records)
        }
    }
}

@Composable
fun PriorityDistributionCard(urgent: Int, normal: Int, total: Int) {
    AnalyticsCard(
        title = stringResource(R.string.priority_distribution),
        icon = Icons.Default.Flag
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val urgentProgress = if (total > 0) urgent.toFloat() / total else 0f
            val normalProgress = if (total > 0) normal.toFloat() / total else 0f

            ProgressBarRow(
                label = stringResource(R.string.urgent),
                count = urgent,
                progress = urgentProgress,
                color = RedUrgent
            )
            ProgressBarRow(
                label = stringResource(R.string.normal),
                count = normal,
                progress = normalProgress,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (total == 0) {
                Text(
                    text = stringResource(R.string.no_data_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ProgressBarRow(label: String, count: Int, progress: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(24.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.1f),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.widthIn(min = 60.dp)
        )
    }
}

@Composable
fun SectorOverviewCard(
    sectorCounts: List<Pair<String, Int>>,
    onSectorClick: (String) -> Unit = {}
) {
    AnalyticsCard(
        title = stringResource(R.string.sector_overview),
        icon = Icons.Default.GridView
    ) {
        if (sectorCounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_data_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val chunkedSectors = sectorCounts.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                chunkedSectors.forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { (sector, count) ->
                            SectorTile(
                                sector = sector,
                                count = count,
                                modifier = Modifier.weight(1f),
                                onClick = { onSectorClick(sector) }
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectorTile(
    sector: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val icon = sectorIcons[sector] ?: Icons.Default.GridView
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(sectorNames[sector] ?: R.string.general_category),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
fun StatusBreakdownCard(records: List<FileRecord>) {
    AnalyticsCard(
        title = stringResource(R.string.status_breakdown),
        icon = Icons.Default.PieChart
    ) {
        val sectorData = remember(records) {
            val counts = mutableMapOf<String, Int>()
            records.forEach { record ->
                record.sectors.forEach { sector ->
                    counts[sector] = counts.getOrDefault(sector, 0) + 1
                }
            }
            val sorted = counts.toList().sortedByDescending { it.second }
            if (sorted.size > 5) {
                val top5 = sorted.take(5)
                val othersCount = sorted.drop(5).sumOf { it.second }
                top5 + ("Other" to othersCount)
            } else {
                sorted
            }
        }

        if (sectorData.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_data_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val total = sectorData.sumOf { it.second }.toFloat()
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isRtl) Arrangement.End else Arrangement.Start
            ) {
                if (isRtl) {
                    ChartLegend(sectorData, Modifier.weight(1f))
                    DonutChart(sectorData, total, Modifier.size(150.dp))
                } else {
                    DonutChart(sectorData, total, Modifier.size(150.dp))
                    ChartLegend(sectorData, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DonutChart(data: List<Pair<String, Int>>, total: Float, modifier: Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    Canvas(modifier = modifier) {
        var startAngle = -90f
        data.forEachIndexed { index, pair ->
            val sweepAngle = (pair.second / total) * 360f
            drawArc(
                color = ChartPalette[index % ChartPalette.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.width, size.height)
            )
            startAngle += sweepAngle
        }
        // Donut hole
        drawCircle(
            color = backgroundColor,
            radius = size.width * 0.3f,
            center = center
        )
    }
}

@Composable
fun ChartLegend(data: List<Pair<String, Int>>, modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.forEachIndexed { index, pair ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(ChartPalette[index % ChartPalette.size]))
                val name = if (pair.first == "Other") stringResource(R.string.other) 
                           else stringResource(sectorNames[pair.first] ?: R.string.general_category)
                Text(
                    text = "$name (${pair.second})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CumulativeCompletionsCard(records: List<FileRecord>) {
    AnalyticsCard(
        title = stringResource(R.string.cumulative_completions),
        icon = Icons.Default.ShowChart
    ) {
        val chartData = remember(records) {
            val completedRecords = records.filter { it.status == "Processed" }
            val grouped = completedRecords.groupBy { it.dateReceivedGov }
                .mapKeys { parseDate(it.key) }
                .filterKeys { it != null }
                .mapKeys { it.key!! }
                .toSortedMap()

            var cumulative = 0
            grouped.map { (date, list) ->
                cumulative += list.size
                date to cumulative
            }
        }

        if (chartData.size < 2) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.not_enough_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LineChart(chartData, Modifier.fillMaxWidth().height(200.dp))
        }
    }
}

@Composable
fun LineChart(data: List<Pair<LocalDate, Int>>, modifier: Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val textPaint = android.graphics.Paint().apply {
        color = onSurface.toArgb()
        textSize = 24f
        textAlign = android.graphics.Paint.Align.CENTER
    }

    Canvas(modifier = modifier.padding(bottom = 24.dp, start = 24.dp, end = 16.dp)) {
        val maxVal = data.maxOf { it.second }.toFloat().coerceAtLeast(1f)
        val minVal = 0f
        val xSpace = size.width / (data.size - 1)
        val ySpace = size.height / (maxVal - minVal)

        val path = Path()
        val fillPath = Path()
        
        data.forEachIndexed { index, pair ->
            val x = index * xSpace
            val y = size.height - (pair.second - minVal) * ySpace
            
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            
            if (index == data.size - 1) {
                fillPath.lineTo(x, size.height)
                fillPath.close()
            }

            // Draw X Axis labels
            if (index % (data.size / 4 + 1) == 0 || index == data.size - 1) {
                drawContext.canvas.nativeCanvas.drawText(
                    pair.first.format(DateTimeFormatter.ofPattern("dd/MM")),
                    x,
                    size.height + 30f,
                    textPaint
                )
            }
        }

        // Draw grid lines
        for (i in 0..4) {
            val y = size.height - (i * (size.height / 4))
            drawLine(outlineVariant.copy(alpha = 0.3f), Offset(0f, y), Offset(size.width, y))
        }

        // Draw area fill
        drawPath(fillPath, primary.copy(alpha = 0.2f))
        
        // Draw line
        drawPath(path, primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        // Draw dots
        data.forEachIndexed { index, pair ->
            val x = index * xSpace
            val y = size.height - (pair.second - minVal) * ySpace
            drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(x, y))
            drawCircle(primary, radius = 4.dp.toPx(), center = Offset(x, y))
        }
    }
}

private fun parseDate(dateStr: String): LocalDate? {
    val formatters = listOf(
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
    )
    return formatters.firstNotNullOfOrNull { formatter ->
        runCatching { LocalDate.parse(dateStr, formatter) }.getOrNull()
    }
}

@Composable
fun AnalyticsCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x14000000)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

private fun Color.toArgb(): Int {
    return (this.alpha * 255.0f + 0.5f).toInt() shl 24 or
           ((this.red * 255.0f + 0.5f).toInt() shl 16) or
           ((this.green * 255.0f + 0.5f).toInt() shl 8) or
           (this.blue * 255.0f + 0.5f).toInt()
}
