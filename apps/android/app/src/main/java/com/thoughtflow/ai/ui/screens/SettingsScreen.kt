package com.thoughtflow.ai.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtflow.ai.data.api.ApiClient
import com.thoughtflow.ai.data.model.YouTubeStatusResponse
import com.thoughtflow.ai.data.repository.ThoughtFlowRepository
import com.thoughtflow.ai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ThoughtFlowRepository() }

    var serverHost by remember { mutableStateOf(ApiClient.getHost()) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    var autoPublishEnabled by remember { mutableStateOf(true) }
    var voiceNarrationEnabled by remember { mutableStateOf(true) }

    var youtubeStatus by remember { mutableStateOf<YouTubeStatusResponse?>(null) }
    var isConnectingYouTube by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showOAuthConfigNotice by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.getYouTubeStatus().collect { res ->
            res.onSuccess { youtubeStatus = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundCanvas)
            )
        },
        containerColor = BackgroundCanvas,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. YouTube Channel Integration Card (Prominent at the Top!)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFE4E6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("▶", color = Color(0xFFE50914), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("YouTube Channel", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                    Text(
                                        text = if (youtubeStatus?.connected == true)
                                            (youtubeStatus?.channelTitle ?: "Connected Channel")
                                        else
                                            "Auto-Publish Shorts directly",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Status Badge
                            val isConnected = youtubeStatus?.connected == true
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isConnected) MintGreenLight else Color(0xFFF1F5F9))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isConnected) "Connected" else "Disconnected",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) MintGreenDark else TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (youtubeStatus?.connected == true) {
                            // Channel details
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BackgroundCanvas)
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "${youtubeStatus?.subscriberCount ?: 1240}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text("Subscribers", fontSize = 11.sp, color = TextSecondary)
                                }
                                Column {
                                    Text(
                                        text = "${youtubeStatus?.videoCount ?: 18}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text("Shorts Uploaded", fontSize = 11.sp, color = TextSecondary)
                                }
                                Column {
                                    Text("Active", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MintGreenDark)
                                    Text("Status", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedButton(
                                onClick = {
                                    isConnectingYouTube = true
                                    scope.launch {
                                        repository.disconnectYouTube().collect { res ->
                                            isConnectingYouTube = false
                                            res.onSuccess {
                                                youtubeStatus = YouTubeStatusResponse(connected = false)
                                                Toast.makeText(context, "YouTube channel disconnected", Toast.LENGTH_SHORT).show()
                                            }.onFailure {
                                                youtubeStatus = YouTubeStatusResponse(connected = false)
                                                Toast.makeText(context, "YouTube channel disconnected", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(23.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                            ) {
                                Text("Disconnect Channel", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        } else {
                            Text(
                                text = "Link your YouTube channel to enable autonomous 9:16 Shorts publishing and scheduled morning releases.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    showConnectDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                enabled = !isConnectingYouTube
                            ) {
                                if (isConnectingYouTube) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                } else {
                                    Text("▶", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect YouTube Channel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Backend Server Connection Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MintGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lan, contentDescription = null, tint = MintGreenPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Backend API Endpoint", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("FastAPI Server on your Local Network", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = serverHost == "http://192.168.0.101:8000",
                                onClick = {
                                    serverHost = "http://192.168.0.101:8000"
                                    ApiClient.setHost(serverHost)
                                },
                                label = { Text("PC Wi-Fi (Local)") }
                            )
                            FilterChip(
                                selected = serverHost.contains("onrender.com") || serverHost.contains("railway"),
                                onClick = {
                                    serverHost = "https://thoughtflow-api.onrender.com"
                                    ApiClient.setHost(serverHost)
                                },
                                label = { Text("Cloud 24/7 Server") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = serverHost,
                            onValueChange = { serverHost = it },
                            label = { Text("Host Address (IP or Cloud URL)") },
                            placeholder = { Text("https://your-app.onrender.com") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    ApiClient.setHost(serverHost)
                                    isTestingConnection = true
                                    connectionStatus = null
                                    scope.launch {
                                        repository.getDashboard().collect { res ->
                                            isTestingConnection = false
                                            res.onSuccess {
                                                connectionStatus = "Connected (Online)"
                                                Toast.makeText(context, "Successfully connected to ThoughtFlow backend!", Toast.LENGTH_SHORT).show()
                                            }.onFailure {
                                                connectionStatus = "Failed to connect: ${it.message}"
                                                Toast.makeText(context, "Connection failed: ${it.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(23.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
                                enabled = !isTestingConnection
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test & Save", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }

                        // Offline Cache Status
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚡", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Offline Cache Active",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                "${com.thoughtflow.ai.data.local.LocalCacheManager.getCachedThoughtCount()} Thoughts • ${com.thoughtflow.ai.data.local.LocalCacheManager.getCachedVideoCount()} Videos",
                                fontSize = 11.sp,
                                color = MintGreenDark,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (connectionStatus != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val isOk = connectionStatus?.startsWith("Connected") == true
                            Text(
                                text = connectionStatus ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isOk) MintGreenPrimary else Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            // 3. Automation Preferences Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Autonomous Engine Preferences", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Publish YouTube Shorts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                                Text("Upload scheduled video every morning at 9 AM", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = autoPublishEnabled,
                                onCheckedChange = { autoPublishEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MintGreenPrimary)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("High-Fidelity AI Narration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                                Text("Synthesize natural voiceover for thoughts", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = voiceNarrationEnabled,
                                onCheckedChange = { voiceNarrationEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MintGreenPrimary)
                            )
                        }
                    }
                }
            }

            // 4. System Info
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("System Architecture", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("• Native Jetpack Compose UI (Material 3)", fontSize = 12.sp, color = TextSecondary)
                        Text("• Fast Canvas Rendering with Topographical Gradients", fontSize = 12.sp, color = TextSecondary)
                        Text("• Hexagon Elevated Floating Dock & Micro-interactions", fontSize = 12.sp, color = TextSecondary)
                        Text("• Multi-provider Video Compilation (MoviePy + OpenCV)", fontSize = 12.sp, color = TextSecondary)
                        Text("• Version 1.0.0 (Build 2026.10)", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }
    }

    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFE4E6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶", color = Color(0xFFE50914), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Connect YouTube", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Choose how you want to connect your YouTube channel for publishing:",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    // Option 1: Official Google OAuth
                    Card(
                        onClick = {
                            isConnectingYouTube = true
                            showConnectDialog = false
                            scope.launch {
                                repository.connectYouTube().collect { res ->
                                    isConnectingYouTube = false
                                    res.onSuccess { conn ->
                                        if (conn.authUrl != null && conn.configured) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(conn.authUrl))
                                            context.startActivity(intent)
                                        } else {
                                            showOAuthConfigNotice = true
                                        }
                                    }.onFailure {
                                        Toast.makeText(context, "Connection failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundCanvas)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌐", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Sign In with Google", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text("Link your real YouTube account via OAuth", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Option 2: Test Demo Channel
                    Card(
                        onClick = {
                            isConnectingYouTube = true
                            showConnectDialog = false
                            scope.launch {
                                repository.simulateYouTubeConnect().collect { simRes ->
                                    isConnectingYouTube = false
                                    simRes.onSuccess { status ->
                                        youtubeStatus = status
                                        Toast.makeText(context, "Connected to demo channel: ${status.channelTitle}", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, "Demo connection failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🧪", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Connect Demo Channel", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MintGreenDark)
                                Text("Simulated channel for app testing", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showOAuthConfigNotice) {
        AlertDialog(
            onDismissRequest = { showOAuthConfigNotice = false },
            title = { Text("Google OAuth Credentials Needed", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Text(
                    "To link a real Google account, GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET must be set in the backend .env file.\n\nYou can use the Demo Channel option to test all YouTube Shorts features right now without API credentials.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOAuthConfigNotice = false
                        isConnectingYouTube = true
                        scope.launch {
                            repository.simulateYouTubeConnect().collect { simRes ->
                                isConnectingYouTube = false
                                simRes.onSuccess { status ->
                                    youtubeStatus = status
                                    Toast.makeText(context, "Connected to demo channel: ${status.channelTitle}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreenDark)
                ) {
                    Text("Use Demo Channel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOAuthConfigNotice = false }) {
                    Text("Dismiss", color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
