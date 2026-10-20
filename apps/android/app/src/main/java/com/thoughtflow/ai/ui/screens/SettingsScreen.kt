package com.thoughtflow.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Check
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings & Connection", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Backend Server Connection Card
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
                                modifier = Modifier.weight(1f).height(46.dp),
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

            // 2. Automation Preferences Card
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

                        Divider(color = Color(0xFFF1F5F9))

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

            // 3. System Info
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
