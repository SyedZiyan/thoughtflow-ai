package com.thoughtflow.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thoughtflow.ai.data.api.ApiClient
import com.thoughtflow.ai.data.local.LocalCacheManager
import com.thoughtflow.ai.ui.components.CreationFormatDialog
import com.thoughtflow.ai.ui.components.FloatingBottomDock
import com.thoughtflow.ai.ui.screens.*
import com.thoughtflow.ai.ui.theme.ThoughtFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalCacheManager.init(applicationContext)
        setContent {
            ThoughtFlowTheme {
                MainAppScreen()
            }
        }
    }
}

private fun getRouteIndex(route: String?): Int {
    if (route == null) return 0
    return when {
        route.startsWith("dashboard") -> 0
        route.startsWith("generator") || route.startsWith("studio") || route.startsWith("create") -> 1
        route.startsWith("library") -> 2
        route.startsWith("analytics") -> 3
        route.startsWith("settings") -> 4
        route.startsWith("calendar") -> 5
        route.startsWith("automation") -> 6
        route.startsWith("logs") -> 7
        route.startsWith("youtube") -> 8
        else -> 0
    }
}

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    var currentTab by remember { mutableStateOf("dashboard") }
    var showDockFormatDialog by remember { mutableStateOf(false) }

    // Sync active tab with current destination
    LaunchedEffect(currentRoute) {
        when {
            currentRoute.startsWith("dashboard") -> currentTab = "dashboard"
            currentRoute.startsWith("generator") || currentRoute.startsWith("studio") -> currentTab = "create"
            currentRoute.startsWith("library") -> currentTab = "library"
            currentRoute.startsWith("analytics") -> currentTab = "analytics"
            currentRoute.startsWith("settings") -> currentTab = "settings"
        }
    }

    // Hide bottom navigation dock on onboarding/auth screens
    val hideBottomBar = currentRoute == "splash" || currentRoute == "login"

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.fillMaxSize(),
            // Silky smooth Apple-style subtle parallax crossfade (zero jarring full-screen whip)
            enterTransition = {
                val initialIdx = getRouteIndex(initialState.destination.route)
                val targetIdx = getRouteIndex(targetState.destination.route)
                val forward = targetIdx >= initialIdx
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> if (forward) fullWidth / 10 else -fullWidth / 10 },
                    animationSpec = tween(
                        durationMillis = 280,
                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 240, easing = LinearOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.98f,
                    animationSpec = tween(durationMillis = 280, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
                )
            },
            exitTransition = {
                val initialIdx = getRouteIndex(initialState.destination.route)
                val targetIdx = getRouteIndex(targetState.destination.route)
                val forward = targetIdx >= initialIdx
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> if (forward) -fullWidth / 12 else fullWidth / 12 },
                    animationSpec = tween(
                        durationMillis = 220,
                        easing = FastOutLinearInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 190)
                ) + scaleOut(
                    targetScale = 0.98f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
                )
            },
            popEnterTransition = {
                val initialIdx = getRouteIndex(initialState.destination.route)
                val targetIdx = getRouteIndex(targetState.destination.route)
                val forward = targetIdx >= initialIdx
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> if (forward) fullWidth / 10 else -fullWidth / 10 },
                    animationSpec = tween(
                        durationMillis = 280,
                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 240, easing = LinearOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.98f,
                    animationSpec = tween(durationMillis = 280, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
                )
            },
            popExitTransition = {
                val initialIdx = getRouteIndex(initialState.destination.route)
                val targetIdx = getRouteIndex(targetState.destination.route)
                val forward = targetIdx >= initialIdx
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> if (forward) -fullWidth / 12 else fullWidth / 12 },
                    animationSpec = tween(
                        durationMillis = 220,
                        easing = FastOutLinearInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 190)
                ) + scaleOut(
                    targetScale = 0.98f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
                )
            }
        ) {
            // Screen 1: Splash / Onboarding
            composable("splash") {
                SplashScreen(
                    onGetStarted = {
                        navController.navigate("dashboard") {
                            popUpTo("splash") { inclusive = true }
                        }
                    },
                    onSignIn = {
                        navController.navigate("login")
                    }
                )
            }

            // Screen 2: Login / Sign Up (Real Identity)
            composable("login") {
                LoginScreen(
                    onNavigateBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate("dashboard")
                        }
                    },
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            // Screen 3: Dashboard
            composable("dashboard") {
                DashboardScreen(
                    onNavigateToStudio = { thoughtId ->
                        val route = if (thoughtId != null) "studio?thoughtId=$thoughtId" else "studio"
                        navController.navigate(route)
                    },
                    onNavigateToGenerator = { mode ->
                        navController.navigate("generator?mode=$mode")
                    },
                    onNavigateToLibrary = {
                        navController.navigate("library")
                    },
                    onNavigateToCalendar = {
                        navController.navigate("calendar")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            // Screen 4: Content Generator (with optional mode=video or mode=photo)
            composable(
                route = "generator?mode={mode}",
                arguments = listOf(
                    navArgument("mode") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = "video"
                    }
                )
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode") ?: "video"
                GeneratorScreen(
                    initialMode = mode,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStudio = { thoughtId ->
                        navController.navigate("studio?thoughtId=$thoughtId")
                    }
                )
            }

            composable("generator") {
                GeneratorScreen(
                    initialMode = "video",
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStudio = { thoughtId ->
                        navController.navigate("studio?thoughtId=$thoughtId")
                    }
                )
            }

            // Screen 5: Video Studio (with optional thoughtId)
            composable(
                route = "studio?thoughtId={thoughtId}",
                arguments = listOf(
                    navArgument("thoughtId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val thoughtId = backStackEntry.arguments?.getString("thoughtId")
                StudioScreen(
                    initialThoughtId = thoughtId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("studio") {
                StudioScreen(
                    initialThoughtId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Screen 6: Content Library
            composable("library") {
                LibraryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPublish = {
                        navController.navigate("youtube")
                    }
                )
            }

            // Screen 7: Publishing Calendar
            composable("calendar") {
                CalendarScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Screen 8: Automation Settings
            composable("automation") {
                AutomationScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Screen 9: Real Channel Analytics
            composable("analytics") {
                AnalyticsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreate = { navController.navigate("generator") }
                )
            }

            // Screen 10: Execution Logs
            composable("logs") {
                LogsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Screen 11: Profile & Settings
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate("dashboard")
                        }
                    },
                    onNavigateToAutomation = { navController.navigate("automation") },
                    onNavigateToLogs = { navController.navigate("logs") },
                    onNavigateToYouTube = { navController.navigate("youtube") },
                    onSignOut = {
                        LocalCacheManager.clearSession()
                        ApiClient.reset()
                        navController.navigate("login") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            // YouTube Channel Hub
            composable("youtube") {
                YouTubeScreen(
                    onNavigateBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate("dashboard")
                        }
                    }
                )
            }
        }

        // 5-Tab Dark Luxury Navigation Dock with Gliding Capsule
        if (!hideBottomBar) {
            FloatingBottomDock(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    when (tab) {
                        "dashboard" -> {
                            currentTab = "dashboard"
                            navController.navigate("dashboard") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        "create" -> {
                            // Ask Photo or Video option when clicking Create!
                            showDockFormatDialog = true
                        }
                        "library" -> {
                            currentTab = "library"
                            navController.navigate("library") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        "analytics" -> {
                            currentTab = "analytics"
                            navController.navigate("analytics") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        "settings" -> {
                            currentTab = "settings"
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }

        // Format Selection Dialog: Ask Photo or Video option when tapping Create on bottom dock
        if (showDockFormatDialog) {
            CreationFormatDialog(
                onDismissRequest = { showDockFormatDialog = false },
                onSelectVideo = {
                    showDockFormatDialog = false
                    currentTab = "create"
                    navController.navigate("generator?mode=video") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSelectPhoto = {
                    showDockFormatDialog = false
                    currentTab = "create"
                    navController.navigate("generator?mode=photo") {
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
