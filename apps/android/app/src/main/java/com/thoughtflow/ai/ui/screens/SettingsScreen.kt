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
                                    isConnectingYouTube = true
                                    scope.launch {
                                        repository.connectYouTube().collect { res ->
                                            isConnectingYouTube = false
                                            res.onSuccess { conn ->
                                                if (conn.authUrl != null && conn.configured) {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(conn.authUrl))
                                                    context.startActivity(intent)
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "YouTube OAuth endpoint ready! In local test mode, dry-run upload simulation is active.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }.onFailure {
                                                Toast.makeText(context, "Connection status: ${it.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
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

                        OutlinedTextField(
                            value = serverHost,
                            onValueChange = { serverHost = it },
                            label = { Text("Host Address (IP:Port)") },
                            placeholder = { Text("http://192.168.0.101:8000") },
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

                            OutlinedButton(
                                onClick = {
                                    serverHost = "http://10.0.2.2:8000"
                                    ApiClient.setHost(serverHost)
                                },
                                modifier = Modifier.height(46.dp),
                                shape = RoundedCornerShape(23.dp)
                            ) {
                                Text("Emulator IP", fontSize = 12.sp)
                            }
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
}
