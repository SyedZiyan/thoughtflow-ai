package com.thoughtflow.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.thoughtflow.ai.data.local.LocalCacheManager
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

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    var currentTab by remember { mutableStateOf("dashboard") }

    // Sync tab with route
    LaunchedEffect(currentRoute) {
        when {
            currentRoute.startsWith("dashboard") -> currentTab = "dashboard"
            currentRoute.startsWith("library") -> currentTab = "library"
            currentRoute.startsWith("youtube") -> currentTab = "activity"
            currentRoute.startsWith("settings") -> currentTab = "settings"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onNavigateToStudio = { thoughtId ->
                        val route = if (thoughtId != null) "studio?thoughtId=$thoughtId" else "studio"
                        navController.navigate(route)
                    },
                    onNavigateToGenerator = {
                        navController.navigate("generator")
                    },
                    onNavigateToLibrary = {
                        navController.navigate("library")
                    }
                )
            }

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

            composable("generator") {
                GeneratorScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStudio = { thoughtId ->
                        navController.navigate("studio?thoughtId=$thoughtId")
                    }
                )
            }

            composable("library") {
                LibraryScreen(
                    onNavigateToPublish = {
                        navController.navigate("youtube")
                    }
                )
            }

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

            composable("settings") {
                SettingsScreen(
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

        // Signature Floating Bottom Dock (Images 1, 2 & 3)
        FloatingBottomDock(
            currentTab = currentTab,
            onTabSelected = { tab ->
                when (tab) {
                    "dashboard", "explore" -> {
                        currentTab = "dashboard"
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
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
                    "activity", "notifications", "youtube" -> {
                        currentTab = "activity"
                        navController.navigate("youtube") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    "settings", "profile" -> {
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
            onActionSelected = { action ->
                when (action) {
                    "generate" -> navController.navigate("generator")
                    "studio" -> navController.navigate("studio")
                    "youtube" -> navController.navigate("youtube")
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}
