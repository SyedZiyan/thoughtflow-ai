package com.thoughtflow.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thoughtflow.ai.data.updater.AppUpdateManager
import com.thoughtflow.ai.ui.components.AiSuggestionCard
import com.thoughtflow.ai.ui.components.HeroSmartCard
import com.thoughtflow.ai.ui.components.UpdateAvailableCard
import com.thoughtflow.ai.ui.theme.*
import com.thoughtflow.ai.viewmodel.DashboardUiState
import com.thoughtflow.ai.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    onNavigateToStudio: (String?) -> Unit,
    onNavigateToGenerator: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val updateState by AppUpdateManager.updateState.collectAsState()

    // Automatically check for over-the-air updates when dashboard loads
    LaunchedEffect(Unit) {
        AppUpdateManager.checkForUpdates(context, silent = true)
    }

    Scaffold(
        containerColor = BackgroundCanvas,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Top Header Bar (From Reference Image 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Grid Menu Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Menu",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center Location/Channel Pill Dropdown (From Reference Image 3: "Weidenhauser... ▾")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable { }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ThoughtFlow AI",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Right Notifications Bell Icon (From Reference Image 3)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            if (updateState.isUpdateAvailable) {
                                Toast.makeText(
                                    context,
                                    "Update v${updateState.latestVersionInfo?.versionName} is ready to install!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                scope.launch {
                                    AppUpdateManager.checkForUpdates(context, silent = false)
                                    Toast.makeText(
                                        context,
                                        updateState.statusMessage ?: "App is up to date!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Alerts",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (updateState.isUpdateAvailable) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-8).dp, y = 8.dp)
                                .clip(CircleShape)
                                .background(MintGreenPrimary)
                        )
                    }
                }
            }

            // 2. Main Content Feed
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MintGreenPrimary)
                    }
                }
                is DashboardUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Unable to connect to ThoughtFlow backend",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = state.message,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                            )
                            Button(
                                onClick = { viewModel.loadData() },
                                colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary)
                            ) {
                                Text("Retry Connection")
                            }
                        }
                    }
                }
                is DashboardUiState.Success -> {
                    val dash = state.dashboard
                    val thoughts = state.thoughts

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // In-App OTA Update Banner (Shows if newer version exists on server)
                        if (updateState.isUpdateAvailable && !updateState.isDismissed) {
                            item {
                                UpdateAvailableCard(
                                    updateState = updateState,
                                    onUpdateClick = {
                                        scope.launch {
                                            AppUpdateManager.downloadAndInstall(context)
                                        }
                                    },
                                    onDismissClick = {
                                        AppUpdateManager.dismissUpdateBanner()
                                    }
                                )
                            }
                        }

                        // Hero Smart Card (From Reference Image 3)
                        item {
                            HeroSmartCard(
                                title = "Daily Shorts Automation",
                                subtitle = "Next publication: ${dash.nextScheduledExecution ?: "Daily at 09:00 AM"}",
                                itemsCount = "+${dash.totalThoughts} thoughts",
                                onNewClick = onNavigateToGenerator,
                                onActionClick = {
                                    Toast.makeText(context, "Running autonomous pipeline...", Toast.LENGTH_SHORT).show()
                                    viewModel.runPipeline { _, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }

                        // AI Suggestion Section Header (From Reference Image 3)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AI Suggestion",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "See more",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MintGreenPrimary,
                                    modifier = Modifier.clickable(onClick = onNavigateToLibrary)
                                )
                            }
                        }

                        // AI Suggestions List (From Reference Image 3: Oat Milk, Eggs, Lemon style)
                        if (thoughts.isEmpty()) {
                            item {
                                AiSuggestionCard(
                                    emoji = "🌱",
                                    title = "Raise Your Daily Standards",
                                    tag = "Discipline",
                                    subtitle = "Protect your non-negotiables with iron commitment.",
                                    onRenderClick = { onNavigateToStudio(null) },
                                    onDismissClick = { }
                                )
                            }
                            item {
                                AiSuggestionCard(
                                    emoji = "⚡",
                                    title = "Don't Stop Until Proud",
                                    tag = "Motivation",
                                    subtitle = "Keep moving forward through every obstacle.",
                                    onRenderClick = { onNavigateToStudio(null) },
                                    onDismissClick = { }
                                )
                            }
                            item {
                                AiSuggestionCard(
                                    emoji = "🌊",
                                    title = "Peace in the Storm",
                                    tag = "Mindfulness",
                                    subtitle = "Not all storms disrupt, some clear your path.",
                                    onRenderClick = { onNavigateToStudio(null) },
                                    onDismissClick = { }
                                )
                            }
                        } else {
                            items(thoughts.take(4)) { item ->
                                val emoji = when (item.theme.lowercase()) {
                                    "discipline" -> "🛡️"
                                    "motivation" -> "🔥"
                                    "resilience" -> "🏔️"
                                    "mindfulness" -> "🧘"
                                    "consistency" -> "⚙️"
                                    else -> "💡"
                                }
                                AiSuggestionCard(
                                    emoji = emoji,
                                    title = item.title,
                                    tag = item.theme.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                    subtitle = item.text,
                                    onRenderClick = { onNavigateToStudio(item.id) },
                                    onDismissClick = { }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
