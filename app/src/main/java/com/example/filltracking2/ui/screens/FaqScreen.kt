package com.example.filltracking2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filltracking2.R

data class FaqItem(
    val category: String,
    val questionRes: Int,
    val answerRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(onNavigateBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var expandedIndex by remember { mutableIntStateOf(-1) }

    val faqItems = listOf(
        // General
        FaqItem(stringResource(R.string.general_category), R.string.faq_q_what_is, R.string.faq_a_what_is),
        FaqItem(stringResource(R.string.general_category), R.string.faq_q_who_can_use, R.string.faq_a_who_can_use),
        FaqItem(stringResource(R.string.general_category), R.string.faq_q_languages, R.string.faq_a_languages),
        // Documents
        FaqItem(stringResource(R.string.documents), R.string.faq_q_register, R.string.faq_a_register),
        FaqItem(stringResource(R.string.documents), R.string.faq_q_priority, R.string.faq_a_priority),
        FaqItem(stringResource(R.string.documents), R.string.faq_q_attachments, R.string.faq_a_attachments),
        FaqItem(stringResource(R.string.documents), R.string.faq_q_find_date, R.string.faq_a_find_date),
        // Dashboard
        FaqItem(stringResource(R.string.dashboard), R.string.faq_q_dashboard_show, R.string.faq_a_dashboard_show),
        FaqItem(stringResource(R.string.dashboard), R.string.faq_q_sector_dash, R.string.faq_a_sector_dash),
        // Security
        FaqItem(stringResource(R.string.security), R.string.faq_q_security, R.string.faq_a_security),
        FaqItem(stringResource(R.string.security), R.string.faq_q_forgot_pw, R.string.faq_a_forgot_pw),
        // Settings
        FaqItem(stringResource(R.string.settings), R.string.faq_q_change_lang, R.string.faq_a_change_lang),
        FaqItem(stringResource(R.string.settings), R.string.faq_q_export, R.string.faq_a_export),
        FaqItem(stringResource(R.string.settings), R.string.faq_q_dark_mode, R.string.faq_a_dark_mode),
        FaqItem(stringResource(R.string.settings), R.string.faq_q_notifications, R.string.faq_a_notifications)
    )

    val categories = listOf(
        stringResource(R.string.all),
        stringResource(R.string.general_category),
        stringResource(R.string.documents),
        stringResource(R.string.dashboard),
        stringResource(R.string.security),
        stringResource(R.string.settings)
    )

    val filteredItems = faqItems.filter { item ->
        val question = stringResource(item.questionRes)
        val matchesSearch = question.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == stringResource(R.string.all) || item.category == selectedCategory
        matchesSearch && matchesCategory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_faq), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Search Bar & Filters centered with max-width ~420dp
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_faq)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Category Tabs (Pills)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                selectedLabelColor = Color(0xFF4CAF50),
                                selectedLeadingIconColor = Color(0xFF4CAF50)
                            ),
                            shape = CircleShape
                        )
                    }
                }
            }

            // FAQ List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 420.dp)
                    .align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems.size) { index ->
                    FaqAccordionItem(
                        item = filteredItems[index],
                        isExpanded = expandedIndex == index,
                        onToggle = {
                            expandedIndex = if (expandedIndex == index) -1 else index
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FaqAccordionItem(
    item: FaqItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "Chevron Rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(item.questionRes),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isExpanded) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                    tint = if (isExpanded) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(item.answerRes),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
