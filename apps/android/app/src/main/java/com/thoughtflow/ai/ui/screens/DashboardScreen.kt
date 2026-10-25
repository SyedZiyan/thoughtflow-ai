package com.thoughtflow.ai.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thoughtflow.ai.data.local.LocalCacheManager
import com.thoughtflow.ai.data.updater.AppUpdateManager
import com.thoughtflow.ai.ui.components.*
import com.thoughtflow.ai.ui.theme.*
import com.thoughtflow.ai.viewmodel.DashboardUiState
import com.thoughtflow.ai.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

/**
 * Screen 3: Dashboard (Elite Polish Edition)
 * - Lotus Logo + ThoughtFlow AI header + Dynamic Personalized User Avatar
 * - Living Cinematic "Today's Thought" hero card with Ken-Burns slow zoom motion
 * - Tactile Specular Status cards with spring bounce physics
 * - Pulsing Scheduled indicator
 * - Next Video Schedule banner
 * - "Generate Now" & "Run Full Pipeline" action buttons
 * - Live OTA In-App update banner
 */
@Composable
fun DashboardScreen(
    onNavigateToStudio: (String?) -> Unit,
    onNavigateToGenerator: (String) -> Unit = {},
    onNavigateToLibrary: () -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val updateState by AppUpdateManager.updateState.collectAsState()

    val userName = remember { LocalCacheManager.getUserName() }
    val userInitial = remember(userName) { userName.take(1).uppercase().ifEmpty { "A" } }

    var showFormatDialog by remember { mutableStateOf(false) }

    // Automatically check for OTA updates on load
    LaunchedEffect(Unit) {
        AppUpdateManager.checkForUpdates(context, silent = true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top Bar: Lotus Logo + ThoughtFlow AI + Dynamic User Avatar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LotusLogo(size = 34.dp)
                    Text(
                        text = "ThoughtFlow AI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                // Dynamic User Avatar with gentle bounce
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PurplePrimary, PurpleDeep)
                            )
                        )
                        .border(1.5.dp, PurpleAccent, CircleShape)
                        .bounceClick(scaleDown = 0.92f, onClick = onNavigateToSettings),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitial,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
            }

            // 2. Main Scrollable Dashboard Content
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    // Elite Shimmer Skeleton Loading
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .shimmerEffect()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .shimmerEffect()
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .shimmerEffect()
                        )
                    }
                }
                is DashboardUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Working in Local Offline Mode",
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "All local features and saved thoughts are available.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                            Button(
                                onClick = { viewModel.loadData() },
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reconnect Backend")
                            }
                        }
                    }
                }
                is DashboardUiState.Success -> {
                    val thoughts = state.thoughts
                    val todayThought = thoughts.firstOrNull()?.text
                        ?: "Small steps create extraordinary change."

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // OTA Update Banner if update available
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

                        // 3. Living Cinematic "Today's Thought" Hero Card with Ken Burns effect
                        item {
                            TodayThoughtHeroCard(
                                quote = todayThought,
                                onCardClick = { showFormatDialog = true }
                            )
                        }

                        // 4. Three Tactile Specular Status Cards
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatusItemCard(
                                    icon = Icons.Outlined.Edit,
                                    title = "Generated\nToday",
                                    statusText = "✓ Completed",
                                    statusColor = StatusGreen,
                                    isPulsing = false,
                                    modifier = Modifier.weight(1f),
                                    onClick = { showFormatDialog = true }
                                )

                                StatusItemCard(
                                    icon = Icons.Outlined.SmartDisplay,
                                    title = "Rendered\nVideo",
                                    statusText = "✓ Completed",
                                    statusColor = StatusGreen,
                                    isPulsing = false,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToStudio(null) }
                                )

                                StatusItemCard(
                                    icon = Icons.Outlined.Subscriptions,
                                    title = "Published\non YouTube",
                                    statusText = "⏰ Scheduled",
                                    statusColor = StatusAmber,
                                    isPulsing = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToCalendar
                                )
                            }
                        }

                        // 5. Specular Next Video Schedule Banner
                        item {
                            NextVideoScheduleBanner(
                                scheduleText = "Tomorrow, 8:00 AM",
                                onViewScheduleClick = onNavigateToCalendar
                            )
                        }

                        // 6. Action Buttons with Spring Tactile Physics
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Primary "Generate Now" Studio Blue Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                            )
                                        )
                                        .bounceClick(scaleDown = 0.96f, onClick = { showFormatDialog = true }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Generate Now",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                }

                                // Secondary "Run Full Pipeline" Clean Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardSurface)
                                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                        .bounceClick(scaleDown = 0.96f, onClick = {
                                            Toast.makeText(context, "Running autonomous pipeline...", Toast.LENGTH_SHORT).show()
                                            viewModel.runPipeline { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Run Full Pipeline",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextWhite
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        // Format Selection Dialog: Ask Photo or Video option
        if (showFormatDialog) {
            CreationFormatDialog(
                onDismissRequest = { showFormatDialog = false },
                onSelectVideo = {
                    showFormatDialog = false
                    onNavigateToGenerator("video")
                },
                onSelectPhoto = {
                    showFormatDialog = false
                    onNavigateToGenerator("photo")
                }
            )
        }
    }
}

/**
 * Living "Today's Thought" Card with cinematic slow Ken Burns camera motion.
 */
@Composable
private fun TodayThoughtHeroCard(
    quote: String,
    onCardClick: () -> Unit
) {
    val specularBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.22f),
            Color(0x448B5CF6),
            Color.White.copy(alpha = 0.05f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color(0x667C3AED))
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, specularBorder, RoundedCornerShape(22.dp))
            .bounceClick(scaleDown = 0.97f, onClick = onCardClick)
    ) {
        // Living cinematic backdrop with Ken Burns motion
        CinematicBackdrop(
            enableKenBurns = true,
            dimAlpha = 0.35f,
            modifier = Modifier.fillMaxSize()
        )

        // Vignette gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x440B0818),
                            Color(0x880B0818),
                            Color(0xEE0B0818)
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Glass Label Chip: "Today's Thought"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF))
                    .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Today's Thought",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
            }

            // Inspiring Quote
            Text(
                text = "\"$quote\"",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                color = TextWhite,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/**
 * Specular Glass Status Card with physical spring bounce
 */
@Composable
private fun StatusItemCard(
    icon: ImageVector,
    title: String,
    statusText: String,
    statusColor: Color,
    isPulsing: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val specularBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.03f)
        )
    )

    // Pulsing halo alpha for scheduled status
    val haloAlpha = if (isPulsing) {
        val transition = rememberInfiniteTransition(label = "pulse_halo")
        val alpha by transition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.40f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        alpha
    } else {
        0.15f
    }

    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Color(0x30000000))
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B1634), Color(0xFF130E26))
                )
            )
            .border(1.dp, specularBorder, RoundedCornerShape(18.dp))
            .bounceClick(scaleDown = 0.94f, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Circle Icon with glowing halo
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = haloAlpha))
                    .border(0.8.dp, statusColor.copy(alpha = haloAlpha * 1.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Specular Next Video Schedule Banner
 */
@Composable
private fun NextVideoScheduleBanner(
    scheduleText: String,
    onViewScheduleClick: () -> Unit
) {
    val specularBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.03f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x25000000))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1432), Color(0xFF120E26))
                )
            )
            .border(1.dp, specularBorder, RoundedCornerShape(16.dp))
            .bounceClick(scaleDown = 0.97f, onClick = onViewScheduleClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = "Schedule",
                    tint = PurpleAccent,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Next Video",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = scheduleText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "View Schedule",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PurplePrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
