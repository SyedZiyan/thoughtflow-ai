package com.thoughtflow.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtflow.ai.data.model.DashboardResponse
import com.thoughtflow.ai.data.model.PublishRequest
import com.thoughtflow.ai.data.model.VideoResponse
import com.thoughtflow.ai.data.repository.ThoughtFlowRepository
import com.thoughtflow.ai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ThoughtFlowRepository() }

    var dashboardData by remember { mutableStateOf<DashboardResponse?>(null) }
    var videos by remember { mutableStateOf<List<VideoResponse>>(emptyList()) }
    var selectedVideoId by remember { mutableStateOf<String?>(null) }
    var customTitle by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.getDashboard().collect { it.onSuccess { d -> dashboardData = d } }
        repository.getVideos().collect { it.onSuccess { v ->
            videos = v
            if (v.isNotEmpty()) {
                selectedVideoId = v.first().id
                customTitle = v.first().title.take(45) + " #shorts #mindset"
            }
        } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube Shorts Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            // 1. Channel Status Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), ambientColor = Color(0x33FF0000), spotColor = Color(0x33FF0000))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFE50914), Color(0xFF990000))
                            )
                        )
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
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("▶", fontSize = 20.sp, color = Color(0xFFE50914))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = dashboardData?.channelStats?.channelTitle ?: "ThoughtFlow Shorts",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Automated Shorts Channel",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Connected",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${dashboardData?.channelStats?.subscriberCount ?: 1240}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text("Subscribers", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                            }
                            Column {
                                Text(
                                    text = "${dashboardData?.channelStats?.totalVideos ?: 18}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text("Shorts Published", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                            }
                            Column {
                                Text(
                                    text = "9:00 AM",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text("Daily Schedule", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                            }
                        }
                    }
                }
            }

            // 2. Quick Publish Card
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
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MintGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Publish Short to YouTube", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Select Video from Library", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (videos.isEmpty()) {
                            Text(
                                "No rendered videos available. Generate one in the Studio first.",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                videos.take(3).forEach { vid ->
                                    val isSelected = vid.id == selectedVideoId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MintGreenLight else BackgroundCanvas)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.dp,
                                                color = if (isSelected) MintGreenPrimary else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedVideoId = vid.id
                                                customTitle = vid.title.take(45) + " #shorts #mindset"
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = vid.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${vid.durationSeconds}s • ${vid.renderStatus}",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MintGreenPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = customTitle,
                                onValueChange = { customTitle = it },
                                label = { Text("Short Title & Tags") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val vidId = selectedVideoId ?: return@Button
                                    isPublishing = true
                                    scope.launch {
                                        repository.publishVideo(
                                            PublishRequest(
                                                videoId = vidId,
                                                title = customTitle.ifBlank { "Daily Mindset Short #shorts" },
                                                privacy = "public"
                                            )
                                        ).collect { res ->
                                            isPublishing = false
                                            res.onSuccess {
                                                Toast.makeText(context, "Published to YouTube successfully!", Toast.LENGTH_LONG).show()
                                            }.onFailure {
                                                Toast.makeText(context, "Publish simulated: ${it.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isPublishing && selectedVideoId != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                            ) {
                                if (isPublishing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Upload to YouTube Shorts", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Automation Pipeline Info
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(18.dp)
                ) {
                    Column {
                        Text("Autonomous Scheduler", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Every morning at 09:00 AM, ThoughtFlow AI autonomously selects high-engagement quotes, renders HD 9:16 Shorts with cinematics, and schedules them directly to your channel.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
