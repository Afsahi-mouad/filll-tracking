package com.example.filltracking2.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filltracking2.R
import com.example.filltracking2.Screen
import com.example.filltracking2.ui.theme.AccentGold
import com.example.filltracking2.ui.theme.MoroccoPrimary
import com.example.filltracking2.ui.theme.ThemeManager

@Composable
fun AppDrawerContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        // Drawer Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else MoroccoPrimary)
                .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    shadowElevation = 0.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.brand),
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.file_tracker),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isDark) AccentGold else Color.White)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.morocco_kingdom),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Spacer(modifier = Modifier.height(16.dp))
        
        // Navigation Items
        val items = listOf(
            DrawerItem(
                label = stringResource(R.string.view_director).uppercase(),
                route = Screen.Dashboard.createRoute(view = "Director"),
                icon = Icons.Default.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                isBold = true
            ),
            DrawerItem(
                label = stringResource(R.string.view_sector),
                route = "sector_view",
                icon = Icons.Outlined.Business,
                unselectedIcon = Icons.Outlined.Business
            ),
            DrawerItem(
                label = stringResource(R.string.analytics),
                route = Screen.Analytics.route,
                icon = Icons.Outlined.BarChart,
                unselectedIcon = Icons.Outlined.BarChart
            )
        )

        items.forEach { item ->
            val selected = currentRoute?.startsWith(item.route.split("?").first()) == true
            NavigationDrawerItem(
                label = { Text(item.label, fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Medium, letterSpacing = if (item.isBold) 1.sp else 0.sp) },
                selected = selected,
                onClick = { onNavigate(item.route); onCloseDrawer() },
                icon = { Icon(if (selected) item.icon else item.unselectedIcon, null) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = (if (isDark) AccentGold else MoroccoPrimary).copy(alpha = if (isDark) 0.2f else 0.12f),
                    selectedIconColor = if (isDark) AccentGold else MoroccoPrimary,
                    selectedTextColor = if (isDark) AccentGold else MoroccoPrimary,
                    unselectedIconColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else MoroccoPrimary,
                    unselectedTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF212121)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Medium) },
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route); onCloseDrawer() },
            icon = { Icon(Icons.Outlined.Settings, null) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = (if (isDark) AccentGold else MoroccoPrimary).copy(alpha = if (isDark) 0.2f else 0.12f),
                selectedIconColor = if (isDark) AccentGold else MoroccoPrimary,
                selectedTextColor = if (isDark) AccentGold else MoroccoPrimary,
                unselectedIconColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else MoroccoPrimary,
                unselectedTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF212121)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private data class DrawerItem(
    val label: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val isBold: Boolean = false
)
