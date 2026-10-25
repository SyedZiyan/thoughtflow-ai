package com.thoughtflow.ai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtflow.ai.data.api.ApiClient
import com.thoughtflow.ai.data.local.LocalCacheManager
import com.thoughtflow.ai.data.model.YouTubeStatusResponse
import com.thoughtflow.ai.data.repository.ThoughtFlowRepository
import com.thoughtflow.ai.data.updater.AppUpdateManager
import com.thoughtflow.ai.ui.components.bounceClick
import com.thoughtflow.ai.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Screen 11: Profile & Settings (Elite Polish Edition)
 * - Tap-to-Edit Personalized Profile Card (saves directly to Local Storage)
 * - Specular Glass menu groups with tactile spring response
 * - YouTube Connection with live status
 * - In-App OTA Update Manager
 * - "Sign Out" button
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAutomation: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToYouTube: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ThoughtFlowRepository() }
    val updateState by AppUpdateManager.updateState.collectAsState()

    var userName by remember { mutableStateOf(LocalCacheManager.getUserName()) }
    var userEmail by remember { mutableStateOf(LocalCacheManager.getUserEmail()) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("English") }
    var notificationsEnabled by remember { mutableStateOf(true) }

    var serverHost by remember { mutableStateOf(ApiClient.getHost()) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var ytStatus by remember { mutableStateOf<YouTubeStatusResponse?>(null) }

    LaunchedEffect(Unit) {
        repository.getYouTubeStatus().collect { res ->
            res.onSuccess { s -> ytStatus = s }
        }
    }

    val userInitial = remember(userName) { userName.take(1).uppercase().ifEmpty { "A" } }

    val specularBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            Color.White.copy(alpha = 0.03f)
        )
    )

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
            // 1. Top Header with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Profile & Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            // 2. Scrollable Settings Feed
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Profile Header Card with Tap-to-Edit
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = PurplePrimary.copy(alpha = 0.2f))
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(CardSurfaceElevated, CardSurface)
                                )
                            )
                            .border(1.dp, specularBorder, RoundedCornerShape(20.dp))
                            .bounceClick(scaleDown = 0.97f, onClick = { showEditProfileDialog = true })
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar initial
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(PurplePrimary, PurpleDeep)
                                        )
                                    )
                                    .border(2.dp, PurpleAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userInitial,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = userEmail,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }

                            // Edit Icon
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = PurpleAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // General Preferences Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x20000000))
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(CardSurfaceElevated, CardSurface)
                                )
                            )
                            .border(1.dp, specularBorder, RoundedCornerShape(20.dp))
                    ) {
                        SettingsMenuItem(
                            icon = Icons.Outlined.Person,
                            title = "Account Settings",
                            onClick = { showEditProfileDialog = true }
                        )

                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                        val isYtLinked = ytStatus?.connected == true
                        SettingsMenuItem(
                            icon = Icons.Outlined.SmartDisplay,
                            title = "YouTube Channel",
                            trailingBadge = if (isYtLinked) (ytStatus?.channelTitle?.take(16) ?: "Connected") else "Not Linked",
                            badgeColor = if (isYtLinked) StatusGreen else StatusAmber,
                            onClick = onNavigateToYouTube
                        )

                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                        SettingsMenuItem(
                            icon = Icons.Outlined.Notifications,
                            title = "Notifications",
                            trailingBadge = if (notificationsEnabled) "Active" else "Muted",
                            badgeColor = if (notificationsEnabled) StatusGreen else TextMuted,
                            onClick = {
                                notificationsEnabled = !notificationsEnabled
                                Toast.makeText(
                                    context,
                                    if (notificationsEnabled) "Notifications enabled" else "Notifications muted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                        SettingsMenuItem(
                            icon = Icons.Outlined.Palette,
                            title = "App Theme",
                            trailingText = AppThemeManager.currentTheme.name,
                            onClick = { showThemeDialog = true }
                        )

                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                        SettingsMenuItem(
                            icon = Icons.Outlined.Language,
                            title = "Language",
                            trailingText = selectedLanguage,
                            onClick = { showLanguageDialog = true }
                        )
                    }
                }

                // Automation & Operations Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x20000000))
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(CardSurfaceElevated, CardSurface)
                                )
                            )
                            .border(1.dp, specularBorder, RoundedCornerShape(20.dp))
                    ) {
                        SettingsMenuItem(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "Automation Settings",
                            onClick = onNavigateToAutomation
                        )

                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                        SettingsMenuItem(
                            icon = Icons.Outlined.Dns,
                            title = "Execution Logs",
                            onClick = onNavigateToLogs
                        )

                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                        SettingsMenuItem(
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            title = "Help & Support",
                            trailingBadge = "Syed Ziyan",
                            badgeColor = Color(0xFF38BDF8),
                            onClick = { showHelpDialog = true }
                        )

                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                        SettingsMenuItem(
                            icon = Icons.Outlined.Info,
                            title = "About ThoughtFlow AI",
                            trailingText = "v1.3.2 Elite",
                            onClick = { showAboutDialog = true }
                        )
                    }
                }

                // In-App OTA Update & Cloud Server Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = PurplePrimary.copy(alpha = 0.25f))
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(CardSurfaceElevated, CardSurface)
                                )
                            )
                            .border(1.dp, PurpleAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        tint = PurplePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "In-App Updates (OTA)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                }

                                Text(
                                    text = "v1.0.9",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PurpleAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = serverHost,
                                onValueChange = {
                                    serverHost = it
                                    ApiClient.setHost(it)
                                },
                                label = { Text("Backend Host / Cloud URL", fontSize = 11.sp, color = TextSecondary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CardSurface,
                                    unfocusedContainerColor = CardSurface,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = PurplePrimary,
                                    unfocusedBorderColor = CardBorder
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardSurface)
                                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                        .bounceClick(scaleDown = 0.95f, onClick = {
                                            isCheckingUpdates = true
                                            scope.launch {
                                                AppUpdateManager.checkForUpdates(context, silent = false)
                                                isCheckingUpdates = false
                                                Toast.makeText(
                                                    context,
                                                    updateState.statusMessage ?: "Up to date!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isCheckingUpdates) "Checking..." else "Check Update",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextWhite
                                    )
                                }

                                if (updateState.isUpdateAvailable) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(PurplePrimary, PurpleDark)
                                                )
                                            )
                                            .bounceClick(scaleDown = 0.95f, onClick = {
                                                scope.launch {
                                                    AppUpdateManager.downloadAndInstall(context)
                                                }
                                            }),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Install Now",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Sign In / Sign Out Button with spring physics
                item {
                    val loggedIn = LocalCacheManager.isLoggedIn()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                12.dp,
                                RoundedCornerShape(14.dp),
                                spotColor = if (loggedIn) Color(0x40EF4444) else Color(0x607C3AED)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (loggedIn) Brush.linearGradient(listOf(StatusRed, Color(0xFFB91C1C)))
                                else Brush.horizontalGradient(listOf(PurplePrimary, PurpleDark))
                            )
                            .bounceClick(scaleDown = 0.96f, onClick = onSignOut),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (loggedIn) "Sign Out of Creator Account" else "Sign In with Real ID",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                    }
                }
            }
        }

        // Tap-to-Edit Profile Dialog
        if (showEditProfileDialog) {
            var tempName by remember { mutableStateOf(userName) }
            var tempEmail by remember { mutableStateOf(userEmail) }

            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                title = { Text("Edit Profile", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("Full Name", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = CardBorder
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tempEmail,
                            onValueChange = { tempEmail = it },
                            label = { Text("Email", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = CardBorder
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempName.isNotBlank()) {
                                userName = tempName.trim()
                                LocalCacheManager.setUserName(userName)
                            }
                            if (tempEmail.isNotBlank()) {
                                userEmail = tempEmail.trim()
                                LocalCacheManager.setUserEmail(userEmail)
                            }
                            showEditProfileDialog = false
                            Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = CardSurfaceElevated,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Help & Support Dialog (Syed Ziyan)
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Help & Support",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Need help with video rendering, YouTube uploads, or feature requests? Contact creator Syed Ziyan directly:",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardSurface)
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "syedziyan9966@gmail.com",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                            }
                        }

                        // Send Email Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = PurplePrimary)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(PurplePrimary, Color(0xFF38BDF8))
                                    )
                                )
                                .bounceClick(scaleDown = 0.96f, onClick = {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:syedziyan9966@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "ThoughtFlow AI - Support & Feedback")
                                        putExtra(Intent.EXTRA_TEXT, "Hello Syed Ziyan,\n\nI need help with:\n")
                                    }
                                    try {
                                        context.startActivity(emailIntent)
                                    } catch (e: Exception) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Support Email", "syedziyan9966@gmail.com"))
                                        Toast.makeText(context, "Copied syedziyan9966@gmail.com to clipboard!", Toast.LENGTH_LONG).show()
                                    }
                                    showHelpDialog = false
                                }),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Email Syed Ziyan", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 13.5.sp)
                        }

                        // Copy Email Button
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Support Email", "syedziyan9966@gmail.com"))
                                Toast.makeText(context, "Copied syedziyan9966@gmail.com to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Email to Clipboard", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text("Close", color = PurpleAccent, fontWeight = FontWeight.SemiBold)
                    }
                },
                containerColor = CardSurfaceElevated,
                shape = RoundedCornerShape(22.dp)
            )
        }

        // About ThoughtFlow AI Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(12.dp, CircleShape, spotColor = PurplePrimary)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(PurplePrimary, Color(0xFFE11D48))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("TF", fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextWhite)
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("ThoughtFlow AI Elite", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Version 1.1.0 (Build 11)", color = PurpleAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Autonomous AI-powered Shorts, Reels, and Photo Quote Studio. Designed for creators who demand elite visual aesthetics and effortless YouTube publishing.",
                            fontSize = 12.5.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardSurface)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Lead Creator", fontSize = 12.sp, color = TextMuted)
                                Text("Syed Ziyan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Developer Contact", fontSize = 12.sp, color = TextMuted)
                                Text("syedziyan9966@gmail.com", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF38BDF8))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Video Engine", fontSize = 12.sp, color = TextMuted)
                                Text("FFmpeg 9:16 + VideoPlayerView", fontSize = 12.sp, color = TextWhite)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("OTA Delivery", fontSize = 12.sp, color = TextMuted)
                                Text("Live In-App Updater", fontSize = 12.sp, color = StatusGreen)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAboutDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }
                },
                containerColor = CardSurfaceElevated,
                shape = RoundedCornerShape(22.dp)
            )
        }

        // App Theme Dialog
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = {
                    Text(
                        "Choose App Theme",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppThemeManager.THEMES.forEach { themeConfig ->
                            val isSelected = AppThemeManager.currentTheme.name == themeConfig.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) themeConfig.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) themeConfig.primary else CardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        AppThemeManager.setTheme(themeConfig.name)
                                        showThemeDialog = false
                                        Toast.makeText(context, "Theme: ${themeConfig.name}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(themeConfig.primary)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(themeConfig.cardSurface)
                                                .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        themeConfig.name,
                                        color = TextWhite,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = themeConfig.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Done", color = PurpleAccent, fontWeight = FontWeight.SemiBold)
                    }
                },
                containerColor = CardSurfaceElevated,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Language Dialog
        if (showLanguageDialog) {
            val languages = listOf("English", "Hindi (हिन्दी)", "Spanish (Español)", "French (Français)", "German (Deutsch)")
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text("Choose Language", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        languages.forEach { lang ->
                            val isSelected = selectedLanguage.startsWith(lang.split(" ")[0])
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PurplePrimary.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (isSelected) PurplePrimary else CardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedLanguage = lang
                                        showLanguageDialog = false
                                        Toast.makeText(context, "Language updated to $lang", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang, color = TextWhite, fontSize = 13.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text("Done", color = PurpleAccent)
                    }
                },
                containerColor = CardSurfaceElevated,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    trailingBadge: String? = null,
    badgeColor: Color = StatusGreen,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingBadge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(0.8.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = trailingBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 13.sp,
                    color = TextMuted,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
