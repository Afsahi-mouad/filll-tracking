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
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filltracking2.R
import com.example.filltracking2.ui.theme.*
import com.example.filltracking2.ui.viewmodel.FileViewModel
import com.example.filltracking2.util.LocaleManager
import com.example.filltracking2.util.PreferenceManager
import com.example.filltracking2.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FileViewModel,
    currentUserEmail: String,
    currentPassword: String,
    onOpenDrawer: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToFaq: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showSupportSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = if (ThemeManager.isDarkTheme) Color.Black else Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (ThemeManager.isDarkTheme) Color.Black else Color(0xFFF5F5F5)
                ),
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        SettingsContent(
            modifier = Modifier.padding(padding),
            viewModel = viewModel,
            currentUserEmail = currentUserEmail,
            currentPassword = currentPassword,
            onSignOut = onSignOut,
            onNavigateToFaq = onNavigateToFaq,
            onShowSupportSheet = { showSupportSheet = true },
            snackbarHostState = snackbarHostState
        )
    }

    if (showSupportSheet) {
        ModalBottomSheet(onDismissRequest = { showSupportSheet = false }) {
            SupportContent(context)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    viewModel: FileViewModel,
    currentUserEmail: String,
    currentPassword: String,
    onSignOut: () -> Unit,
    onNavigateToFaq: () -> Unit,
    onShowSupportSheet: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentLocale = LocaleManager.LocalAppLocale.current
    val records by viewModel.records.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let { viewModel.exportToExcel(it) }
    }

    val languageMap = mapOf(
        "en" to "English",
        "fr" to "Français",
        "ar" to "العربية",
        "de" to "Deutsch",
        "es" to "Español"
    )

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // User Profile Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (ThemeManager.isDarkTheme) DarkSurface else MoroccoPrimary.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = if (ThemeManager.isDarkTheme) AccentGold else MoroccoPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = currentUserEmail.take(1).uppercase(),
                            color = if (ThemeManager.isDarkTheme) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = currentUserEmail,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (ThemeManager.isDarkTheme) Color.White else Color.Black
                    )
                    Text(
                        text = "Administrator",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ThemeManager.isDarkTheme) AccentGold else MoroccoPrimary
                    )
                }
            }
        }

        // Preferences Group
        SettingsSection(title = stringResource(R.string.preferences)) {
            var showLanguageDialog by remember { mutableStateOf(false) }
            SettingsItem(
                icon = Icons.Default.Public,
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

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))

            SettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.theme),
                showChevron = false,
                control = {
                    Switch(
                        checked = ThemeManager.isDarkTheme,
                        onCheckedChange = { ThemeManager.isDarkTheme = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (ThemeManager.isDarkTheme) AccentGold else Color(0xFF1565C0)
                        )
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))

            SettingsItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.notifications),
                showChevron = false,
                control = {
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
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (ThemeManager.isDarkTheme) AccentGold else Color(0xFF1565C0)
                        )
                    )
                }
            )
        }

        // Account Group
        SettingsSection(title = stringResource(R.string.account)) {
            var showPasswordForm by remember { mutableStateOf(false) }
            SettingsItem(
                icon = Icons.Default.MoreHoriz,
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

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Help,
                title = stringResource(R.string.contact_support),
                onClick = onShowSupportSheet
            )
        }

        // Data & Storage
        SettingsSection(title = "Data & Storage") {
            val isExporting = exportState is FileViewModel.ExportState.Loading
            
            SettingsItem(
                icon = Icons.Default.FileDownload,
                title = stringResource(R.string.export_excel),
                subtitle = stringResource(R.string.export_data),
                onClick = {
                    if (records.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.no_records_to_export))
                        }
                    } else {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        createDocumentLauncher.launch("FillTracking_$date.xlsx")
                    }
                },
                control = if (isExporting) { { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) } } else null
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))

            SettingsInfoItem(
                icon = Icons.Default.Storage,
                title = "App Storage",
                value = getFolderSizeLabel(context.filesDir)
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))

            SettingsInfoItem(
                icon = Icons.Default.SdStorage,
                title = "Device Storage",
                value = getDeviceStorageInfo()
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))

            // Wipe Data Button
            var showWipeDialog by remember { mutableStateOf(false) }
            
            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = stringResource(R.string.wipe_data),
                titleColor = MaterialTheme.colorScheme.error,
                showChevron = false,
                onClick = { showWipeDialog = true }
            )

            if (showWipeDialog) {
                AlertDialog(
                    onDismissRequest = { showWipeDialog = false },
                    title = { Text(stringResource(R.string.wipe_confirm_title)) },
                    text = { Text(stringResource(R.string.wipe_confirm_msg)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.wipeAllData {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.wipe_success))
                                    }
                                }
                                showWipeDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWipeDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }

        // About Group
        SettingsSection(title = stringResource(R.string.about)) {
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Help,
                title = stringResource(R.string.help_faq),
                onClick = onNavigateToFaq
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))

            SettingsItem(
                icon = Icons.Default.Info,
                title = "Version 3.0.0",
                subtitle = stringResource(R.string.copyright),
                showChevron = false
            )
        }

        // Sign Out
        SettingsSection(title = "Session") {
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = stringResource(R.string.sign_out),
                titleColor = MaterialTheme.colorScheme.error,
                showChevron = false,
                onClick = {
                    scope.launch {
                        PreferenceManager.setLoggedIn(context, false)
                        onSignOut()
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ChangePasswordForm(currentStoredPassword: String, onPasswordUpdated: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    
    var error by remember { mutableStateOf<String?>(null) }
    
    var showSecurityAlert by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = currentInput,
            onValueChange = { currentInput = it; error = null },
            label = { Text(stringResource(R.string.current_password)) },
            visualTransformation = if (currentVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { currentVisible = !currentVisible }) {
                    Icon(if (currentVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it; error = null },
            label = { Text(stringResource(R.string.new_password)) },
            visualTransformation = if (newVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { newVisible = !newVisible }) {
                    Icon(if (newVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; error = null },
            label = { Text(stringResource(R.string.confirm_password)) },
            visualTransformation = if (confirmVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                    Icon(if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
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
fun SupportContent(context: Context) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Help,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (ThemeManager.isDarkTheme) AccentGold else MoroccoPrimary
        )

        Text(
            "Support",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (ThemeManager.isDarkTheme) AccentGold else Color(0xFF1565C0),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth(),
            textAlign = if (LocaleManager.LocalAppLocale.current == "ar") TextAlign.End else TextAlign.Start
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (ThemeManager.isDarkTheme) DarkSurface else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = if (ThemeManager.isDarkTheme) BorderStroke(1.dp, AccentGold.copy(alpha = 0.1f)) else null
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
    titleColor: Color = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF212121),
    showChevron: Boolean = true,
    control: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val isRtl = LocaleManager.LocalAppLocale.current == "ar"
    
    CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (titleColor == MaterialTheme.colorScheme.error) titleColor else if (ThemeManager.isDarkTheme) AccentGold else Color(0xFF424242),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = if (ThemeManager.isDarkTheme) TextSecondaryDark else Color.Gray
                    )
                }
            }

            if (control != null) {
                control()
            } else if (showChevron) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null, 
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    val isRtl = LocaleManager.LocalAppLocale.current == "ar"
    CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (ThemeManager.isDarkTheme) AccentGold else Color(0xFF424242),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title, 
                style = MaterialTheme.typography.bodyLarge, 
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value, 
                style = MaterialTheme.typography.bodyMedium, 
                fontWeight = FontWeight.Bold,
                color = if (ThemeManager.isDarkTheme) AccentGold else MaterialTheme.colorScheme.primary
            )
        }
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
