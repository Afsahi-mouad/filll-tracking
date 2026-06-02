package com.example.filltracking2

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.filltracking2.ui.screens.*
import com.example.filltracking2.ui.theme.*
import com.example.filltracking2.ui.viewmodel.FileViewModel
import com.example.filltracking2.util.PreferenceManager
import kotlinx.coroutines.delay
import com.example.filltracking2.util.LocaleManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : Screen("dashboard?view={view}&sector={sector}", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard) {
        fun createRoute(view: String? = null, sector: String? = null): String {
            val viewPart = if (view != null) "view=$view" else ""
            val sectorPart = if (sector != null) "sector=$sector" else ""
            val query = listOf(viewPart, sectorPart).filter { it.isNotEmpty() }.joinToString("&")
            return if (query.isNotEmpty()) "dashboard?$query" else "dashboard"
        }
    }
    object History : Screen("history", "History", Icons.Filled.History, Icons.Outlined.History)
    object Analytics : Screen("analytics", "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    object SectorView : Screen("sector_view", "Sector View", Icons.Filled.Business, Icons.Outlined.Business) {
        fun createRoute(sectorName: String) = if (sectorName.isEmpty()) "sector_view" else "sector_view/$sectorName"
    }
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillTrackingApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileViewModel: FileViewModel = viewModel()
    
    val isLoggedInPersisted by PreferenceManager.isLoggedIn(context).collectAsStateWithLifecycle(initialValue = false)
    val currentUserEmailPersisted by PreferenceManager.getCurrentUserEmail(context).collectAsStateWithLifecycle(initialValue = "")
    
    var isLoggedIn by rememberSaveable(isLoggedInPersisted) { mutableStateOf(isLoggedInPersisted) }
    var currentUserEmail by rememberSaveable(currentUserEmailPersisted) { mutableStateOf(currentUserEmailPersisted) }

    val currentLocaleCode by PreferenceManager.getLocale(context).collectAsStateWithLifecycle(initialValue = null)
    
    var isSplashLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1500)
        isSplashLoading = false
    }

    LaunchedEffect(currentLocaleCode) {
        currentLocaleCode?.let {
            PreferenceManager.applyLocale(it)
        }
    }

    if (currentLocaleCode == null || isSplashLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.brand),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(180.dp),
                    contentScale = ContentScale.Fit
                )
                CircularProgressIndicator(color = Color(0xFF004824))
            }
        }
        return
    }

    val activity = remember(context) {
        var c = context
        while (c is android.content.ContextWrapper) {
            if (c is androidx.activity.ComponentActivity) break
            c = c.baseContext
        }
        c as? androidx.activity.ComponentActivity
    }

    if (activity == null) {
        // Fallback or show error
        return
    }

    CompositionLocalProvider(
        LocalContext provides LocaleManager.wrapContext(context, currentLocaleCode!!),
        LocalActivityResultRegistryOwner provides activity,
        LocaleManager.LocalAppLocale provides currentLocaleCode!!
    ) {
        val persistedPassword by PreferenceManager
            .getPassword(LocalContext.current)
            .collectAsStateWithLifecycle(initialValue = "admin")

        FillTrackingTheme(darkTheme = ThemeManager.isDarkTheme) {
            if (!isLoggedIn) {
                var showFaqOnLogin by remember { mutableStateOf(false) }
                if (showFaqOnLogin) {
                    FaqScreen(onNavigateBack = { showFaqOnLogin = false })
                } else {
                    LoginScreen(
                        onLoginSuccess = { email ->
                            scope.launch {
                                PreferenceManager.setLoggedIn(context, true, email)
                                currentUserEmail = email
                                isLoggedIn = true
                            }
                        },
                        onNavigateToFaq = { showFaqOnLogin = true }
                    )
                }
            } else {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                val bottomNavItems = listOf(Screen.Dashboard, Screen.History, Screen.Analytics, Screen.SectorView)
                val showBottomBar = currentDestination?.route?.split("?")?.firstOrNull() in bottomNavItems.map { it.route.split("?").first() }
                
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val drawerSnackbarHostState = remember { SnackbarHostState() }
                var showSupportSheetInDrawer by remember { mutableStateOf(false) }

                if (showSupportSheetInDrawer) {
                    ModalBottomSheet(onDismissRequest = { showSupportSheetInDrawer = false }) {
                        SupportContent(LocalContext.current)
                    }
                }
                
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(320.dp).fillMaxHeight(),
                            drawerContainerColor = if (ThemeManager.isDarkTheme) Color.Black else Color(0xFFF5F5F5),
                            drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "SUIVI DES FICHIERS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MoroccoPrimary,
                                    modifier = Modifier.padding(16.dp)
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                                // Settings Content integrated directly
                                SettingsContent(
                                    viewModel = fileViewModel,
                                    currentUserEmail = currentUserEmail,
                                    currentPassword = persistedPassword,
                                    onSignOut = { isLoggedIn = false },
                                    onNavigateToFaq = { 
                                        scope.launch { drawerState.close() }
                                        navController.navigate("faq") 
                                    },
                                    onShowSupportSheet = { 
                                        showSupportSheetInDrawer = true 
                                    },
                                    snackbarHostState = drawerSnackbarHostState
                                )
                            }
                        }
                    }
                ) {
                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 3.dp
                                ) {
                                    bottomNavItems.forEach { screen ->
                                        val selected = currentDestination?.hierarchy?.any { 
                                            it.route?.split("?")?.firstOrNull() == screen.route.split("?").first()
                                        } == true
                                        
                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    if (selected) screen.selectedIcon else screen.unselectedIcon,
                                                    contentDescription = screen.title
                                                )
                                            },
                                            label = {
                                                val stringResId = when(screen.route) {
                                                    "dashboard" -> R.string.dashboard
                                                    "history" -> R.string.history
                                                    "analytics" -> R.string.analytics
                                                    "sector_view" -> R.string.sector_dashboard
                                                    else -> R.string.home
                                                }
                                                Text(stringResource(stringResId))
                                            },
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            if (showBottomBar && currentDestination?.route?.startsWith("dashboard") == true) {
                                FloatingActionButton(
                                    onClick = { navController.navigate("new_file") },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Icon(Icons.Default.Add, "Add new file")
                                }
                            }
                        }
                    ) { padding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Dashboard.route,
                            modifier = Modifier.padding(padding),
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            }
                        ) {
                            composable(
                                route = Screen.Dashboard.route,
                                arguments = listOf(
                                    navArgument("view") { type = NavType.StringType; nullable = true },
                                    navArgument("sector") { type = NavType.StringType; nullable = true }
                                )
                            ) { backStackEntry ->
                                val viewArg = backStackEntry.arguments?.getString("view") ?: "Director"
                                val sectorArg = backStackEntry.arguments?.getString("sector")
                                
                                DashboardScreen(
                                    navController = navController,
                                    viewModel = fileViewModel,
                                    initialView = viewArg,
                                    initialSector = sectorArg,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onFileClick = { record ->
                                        val encoded = Uri.encode(record.internalSerial)
                                        navController.navigate("file_detail/$encoded")
                                    }
                                )
                            }
                            composable(Screen.History.route) {
                                HistoryScreen(
                                    viewModel = fileViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onFileClick = { record ->
                                        val encoded = Uri.encode(record.internalSerial)
                                        navController.navigate("file_detail/$encoded")
                                    }
                                )
                            }
                            composable(Screen.Analytics.route) {
                                AnalyticsScreen(
                                    viewModel = fileViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onSectorClick = { sector ->
                                        navController.navigate(Screen.SectorView.createRoute(sector))
                                    }
                                )
                            }
                            composable(Screen.SectorView.route) {
                                SectorViewScreen(
                                    viewModel = fileViewModel,
                                    filterBySector = "",
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onFileClick = { record ->
                                        val encoded = Uri.encode(record.internalSerial)
                                        navController.navigate("file_detail/$encoded")
                                    },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = "sector_view/{sectorName}",
                                arguments = listOf(navArgument("sectorName") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val sectorName = backStackEntry.arguments?.getString("sectorName") ?: ""
                                SectorViewScreen(
                                    viewModel = fileViewModel,
                                    filterBySector = sectorName,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onFileClick = { record ->
                                        val encoded = Uri.encode(record.internalSerial)
                                        navController.navigate("file_detail/$encoded")
                                    },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    viewModel = fileViewModel,
                                    currentUserEmail = currentUserEmail,
                                    currentPassword = persistedPassword,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onSignOut = { isLoggedIn = false },
                                    onNavigateToFaq = { navController.navigate("faq") }
                                )
                            }
                            composable("faq") {
                                FaqScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable("new_file") {
                                NewFileScreen(
                                    viewModel = fileViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onOpenImageViewer = { navController.navigate("image_viewer") }
                                )
                            }
                            composable(
                                route = "edit_file/{serial}",
                                arguments = listOf(navArgument("serial") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val serial = backStackEntry.arguments?.getString("serial") ?: ""
                                val decodedSerial = Uri.decode(serial)
                                NewFileScreen(
                                    viewModel = fileViewModel,
                                    editingRecordId = decodedSerial,
                                    onNavigateBack = { navController.popBackStack() },
                                    onOpenImageViewer = { navController.navigate("image_viewer") }
                                )
                            }
                            composable(
                                route = "file_detail/{serial}",
                                arguments = listOf(navArgument("serial") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val serial = backStackEntry.arguments?.getString("serial") ?: ""
                                val decodedSerial = Uri.decode(serial)
                                FileDetailScreen(
                                    serial = decodedSerial,
                                    viewModel = fileViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onEditFile = { internalSerial ->
                                        val encoded = Uri.encode(internalSerial)
                                        navController.navigate("edit_file/$encoded")
                                    },
                                    onOpenImageViewer = { navController.navigate("image_viewer") }
                                )
                            }
                            composable(
                                route = "image_viewer",
                                enterTransition = { fadeIn(animationSpec = tween(300)) },
                                exitTransition = { fadeOut(animationSpec = tween(300)) }
                            ) {
                                val images by fileViewModel.viewerImages.collectAsStateWithLifecycle()
                                val initialIndex by fileViewModel.viewerInitialIndex.collectAsStateWithLifecycle()
                                
                                ImageDetailScreen(
                                    imagePaths = images,
                                    initialIndex = initialIndex,
                                    onClose = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
