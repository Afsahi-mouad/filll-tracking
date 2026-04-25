package com.example.filltracking2

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.ui.res.stringResource
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
import com.example.filltracking2.ui.theme.FillTrackingTheme
import com.example.filltracking2.ui.viewmodel.FileViewModel
import com.example.filltracking2.ui.theme.ThemeManager
import com.example.filltracking2.util.PreferenceManager
import com.example.filltracking2.util.LocaleManager

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object History : Screen("history", "History", Icons.Filled.History, Icons.Outlined.History)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillTrackingApp() {
    val context = LocalContext.current
    val fileViewModel: FileViewModel = viewModel()
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var currentUserEmail by rememberSaveable { mutableStateOf("") }

    val currentLocaleCode by PreferenceManager.getLocale(context).collectAsStateWithLifecycle(initialValue = "en")

    val activity = remember(context) {
        var c = context
        while (c is android.content.ContextWrapper) {
            if (c is androidx.activity.ComponentActivity) break
            c = c.baseContext
        }
        c as? androidx.activity.ComponentActivity
    }

    CompositionLocalProvider(
        LocalContext provides LocaleManager.wrapContext(context, currentLocaleCode),
        LocalActivityResultRegistryOwner provides activity!!,
        LocaleManager.LocalAppLocale provides currentLocaleCode
    ) {
        val persistedPassword by PreferenceManager
            .getPassword(LocalContext.current)
            .collectAsStateWithLifecycle(initialValue = "admin")

        if (!isLoggedIn) {
            LoginScreen(
                currentPassword = persistedPassword,
                onLoginSuccess = { email ->
                    currentUserEmail = email
                    isLoggedIn = true
                }
            )
        } else {
            FillTrackingTheme(darkTheme = ThemeManager.isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                val bottomNavItems = listOf(Screen.Dashboard, Screen.History, Screen.Settings)
                val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }
                
                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                bottomNavItems.forEach { screen ->
                                    val selected = currentDestination?.hierarchy?.any { 
                                        it.route == screen.route 
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
                                                "settings" -> R.string.settings
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
                        if (showBottomBar && currentDestination?.route == Screen.Dashboard.route) {
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
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                viewModel = fileViewModel,
                                onFileClick = { record ->
                                    val encoded = Uri.encode(record.internalSerial)
                                    navController.navigate("file_detail/$encoded")
                                }
                            )
                        }
                        composable(Screen.History.route) {
                            HistoryScreen(
                                viewModel = fileViewModel,
                                onFileClick = { record ->
                                    val encoded = Uri.encode(record.internalSerial)
                                    navController.navigate("file_detail/$encoded")
                                }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                currentUserEmail = currentUserEmail,
                                currentPassword = persistedPassword,
                                onSignOut = { isLoggedIn = false }
                            )
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
