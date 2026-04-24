package com.example.filltracking2.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.filltracking2.ui.theme.ThemeManager
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUserEmail: String,
    currentPassword: String,
    onPasswordChanged: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showSupportSheet by remember { mutableStateOf(false) }

    // Permission handling - now synced with system state
    var notificationsEnabled by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationsEnabled = true
        } else {
            notificationsEnabled = false
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ThemeManager.getString("settings"), fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // User Profile Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = currentUserEmail,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Administrator",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Preferences Group
            SettingsSection(title = ThemeManager.getString("preferences")) {
                // Language
                var showLanguageDialog by remember { mutableStateOf(false) }
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = ThemeManager.getString("language"),
                    subtitle = ThemeManager.currentLanguage,
                    onClick = { showLanguageDialog = true }
                )
                if (showLanguageDialog) {
                    OptionDialog(
                        title = ThemeManager.getString("language"),
                        options = listOf("English", "Français", "العربية", "Deutsch", "Español"),
                        onDismiss = { showLanguageDialog = false },
                        onSelect = { ThemeManager.currentLanguage = it; showLanguageDialog = false }
                    )
                }

                // Theme
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(ThemeManager.getString("theme"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = ThemeManager.isDarkTheme,
                        onCheckedChange = { ThemeManager.isDarkTheme = it }
                    )
                }

                // Notifications
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(ThemeManager.getString("notifications"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val status = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.POST_NOTIFICATIONS
                                    )
                                    if (status == PackageManager.PERMISSION_GRANTED) {
                                        notificationsEnabled = true
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    notificationsEnabled = true
                                }
                            } else {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }

            // Account Group
            SettingsSection(title = ThemeManager.getString("account")) {
                var showPasswordForm by remember { mutableStateOf(false) }
                SettingsItem(
                    icon = Icons.Default.Password,
                    title = ThemeManager.getString("change_password"),
                    onClick = { showPasswordForm = !showPasswordForm }
                )
                
                AnimatedVisibility(visible = showPasswordForm) {
                    ChangePasswordForm(
                        currentStoredPassword = currentPassword,
                        onPasswordUpdated = { 
                            onPasswordChanged(it)
                            showPasswordForm = false
                        }
                    )
                }

                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.ContactSupport,
                    title = ThemeManager.getString("contact_support"),
                    onClick = { showSupportSheet = true }
                )

                SettingsItem(
                    icon = Icons.Default.Star,
                    title = ThemeManager.getString("rate_app"),
                    onClick = { 
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Afsahi-mouad/filll-tracking"))
                        context.startActivity(intent)
                    }
                )

                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = ThemeManager.getString("sign_out"),
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = onSignOut
                )
            }

            // Data & Storage
            SettingsSection(title = "Data & Storage") {
                SettingsInfoItem(
                    icon = Icons.Default.Storage,
                    title = "App Storage",
                    value = getFolderSizeLabel(context.filesDir)
                )
                SettingsInfoItem(
                    icon = Icons.Default.SdStorage,
                    title = "Device Storage",
                    value = getDeviceStorageInfo()
                )
            }

            // About Group
            SettingsSection(title = ThemeManager.getString("about")) {
                SettingsItem(icon = Icons.AutoMirrored.Filled.Help, title = "Help / FAQ")
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(ThemeManager.getString("version"), modifier = Modifier.weight(1f))
                    Text("1.0.0", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Support Bottom Sheet
    if (showSupportSheet) {
        ModalBottomSheet(onDismissRequest = { showSupportSheet = false }) {
            SupportContent(context)
        }
    }
}

@Composable
fun ChangePasswordForm(currentStoredPassword: String, onPasswordUpdated: (String) -> Unit) {
    var currentInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = currentInput,
            onValueChange = { currentInput = it; error = null },
            label = { Text(ThemeManager.getString("current_password")) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it; error = null },
            label = { Text(ThemeManager.getString("new_password")) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; error = null },
            label = { Text(ThemeManager.getString("confirm_password")) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        if (success) Text(ThemeManager.getString("success"), color = Color(0xFF4CAF50), fontSize = 12.sp)

        Button(
            onClick = {
                if (currentInput != currentStoredPassword) {
                    error = ThemeManager.getString("error_wrong")
                } else if (newPassword != confirmPassword) {
                    error = ThemeManager.getString("error_match")
                } else {
                    onPasswordUpdated(newPassword)
                    success = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(ThemeManager.getString("update"))
        }
    }
}

@Composable
fun SupportContent(context: android.content.Context) {
    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Support", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        
        SupportItem(icon = Icons.Default.Email, label = "Email", value = "mouadafs@gmail.com") {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:mouadafs@gmail.com")
            }
            context.startActivity(intent)
        }
        SupportItem(icon = Icons.Default.Phone, label = "Phone", value = "0667161662") {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:0667161662")
            }
            context.startActivity(intent)
        }
        SupportItem(icon = Icons.Default.Code, label = "GitHub", value = "Afsahi-mouad") {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Afsahi-mouad"))
            context.startActivity(intent)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SupportItem(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (titleColor == MaterialTheme.colorScheme.error) titleColor else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OptionDialog(
    title: String,
    options: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Text(
                        text = option,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Storage Helpers
fun getFolderSizeLabel(file: File): String {
    var size = 0L
    if (file.isDirectory) {
        file.listFiles()?.forEach { size += it.length() }
    } else {
        size = file.length()
    }
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return if (mb > 1) String.format(Locale.ENGLISH, "%.1f MB", mb) else String.format(Locale.ENGLISH, "%.1f KB", kb)
}

fun getDeviceStorageInfo(): String {
    val stat = StatFs(Environment.getDataDirectory().path)
    val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
    val gb = bytesAvailable / (1024.0 * 1024.0 * 1024.0)
    return String.format(Locale.ENGLISH, "%.1f GB Free", gb)
}
