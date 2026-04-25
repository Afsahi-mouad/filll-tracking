package com.example.filltracking2.ui.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.filltracking2.R
import com.example.filltracking2.ui.theme.ThemeManager
import com.example.filltracking2.util.PreferenceManager
import com.example.filltracking2.util.LocaleManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUserEmail: String,
    currentPassword: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var showSupportSheet by remember { mutableStateOf(false) }

    val currentLocale = LocaleManager.LocalAppLocale.current
    
    val languageMap = mapOf(
        "en" to "English",
        "fr" to "Français",
        "ar" to "العربية",
        "de" to "Deutsch",
        "es" to "Español"
    )

    // Permission handling - now synced with system state
    var notificationsEnabled by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsEnabled = isGranted
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
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) }
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
            SettingsSection(title = stringResource(R.string.preferences)) {
                // Language
                var showLanguageDialog by remember { mutableStateOf(false) }
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.language),
                    subtitle = languageMap[currentLocale] ?: "English",
                    onClick = { showLanguageDialog = true }
                )
                if (showLanguageDialog) {
                    OptionDialog(
                        title = stringResource(R.string.language),
                        options = languageMap.values.toList(),
                        onDismiss = { showLanguageDialog = false },
                        onSelect = { selectedName ->
                            val code = languageMap.filterValues { it == selectedName }.keys.firstOrNull() ?: "en"
                            scope.launch {
                                PreferenceManager.setLocale(context, code)
                            }
                            showLanguageDialog = false
                        }
                    )
                }

                // Theme
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.theme), modifier = Modifier.weight(1f))
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
                    Text(stringResource(R.string.notifications), modifier = Modifier.weight(1f))
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
            SettingsSection(title = stringResource(R.string.account)) {
                var showPasswordForm by remember { mutableStateOf(false) }
                SettingsItem(
                    icon = Icons.Default.Password,
                    title = stringResource(R.string.change_password),
                    onClick = { showPasswordForm = !showPasswordForm }
                )
                
                AnimatedVisibility(visible = showPasswordForm) {
                    ChangePasswordForm(
                        currentStoredPassword = currentPassword,
                        onPasswordUpdated = { 
                            showPasswordForm = false
                        }
                    )
                }

                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.ContactSupport,
                    title = stringResource(R.string.contact_support),
                    onClick = { showSupportSheet = true }
                )

                SettingsItem(
                    icon = Icons.Default.Star,
                    title = stringResource(R.string.rate_app),
                    onClick = { 
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Afsahi-mouad/filll-tracking"))
                        context.startActivity(intent)
                    }
                )

                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = stringResource(R.string.sign_out),
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
            SettingsSection(title = stringResource(R.string.about)) {
                SettingsItem(icon = Icons.AutoMirrored.Filled.Help, title = "Help / FAQ")
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Version 2.3", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.copyright), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    var showSecurityAlert by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = currentInput,
            onValueChange = { currentInput = it; error = null },
            label = { Text(stringResource(R.string.current_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it; error = null },
            label = { Text(stringResource(R.string.new_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; error = null },
            label = { Text(stringResource(R.string.confirm_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

        Button(
            onClick = {
                if (currentInput != currentStoredPassword) {
                    error = context.getString(R.string.error_wrong)
                } else if (newPassword != confirmPassword) {
                    error = context.getString(R.string.error_match)
                } else {
                    scope.launch {
                        PreferenceManager.setPassword(context, newPassword)
                        showSecurityAlert = true
                        showChangeNotification(context)
                        onPasswordUpdated(newPassword)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.update))
        }
    }

    if (showSecurityAlert) {
        AlertDialog(
            onDismissRequest = { showSecurityAlert = false },
            icon = { Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.security_alert)) },
            text = { Text(stringResource(R.string.security_desc)) },
            confirmButton = {
                TextButton(onClick = { showSecurityAlert = false }) {
                    Text("OK")
                }
            }
        )
    }
}

private fun showChangeNotification(context: Context) {
    val channelId = "security_alerts"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Security Alerts", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setContentTitle(context.getString(R.string.security_alert))
        .setContentText(context.getString(R.string.password_changed))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(1001, notification)
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
