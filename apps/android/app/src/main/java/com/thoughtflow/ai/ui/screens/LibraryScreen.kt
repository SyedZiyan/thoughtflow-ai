package com.thoughtflow.ai.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.VideoLibrary
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.thoughtflow.ai.data.api.ApiClient
import com.thoughtflow.ai.data.model.VideoResponse
import com.thoughtflow.ai.data.repository.ThoughtFlowRepository
import com.thoughtflow.ai.ui.theme.*

@Composable
fun LibraryScreen(
    onNavigateToPublish: (VideoResponse) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ThoughtFlowRepository() }
    var videos by remember { mutableStateOf<List<VideoResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var playingVideo by remember { mutableStateOf<VideoResponse?>(null) }

    LaunchedEffect(Unit) {
        repository.getVideos().collect { res ->
            isLoading = false
            res.onSuccess { videos = it }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundCanvas)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("Content Library", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "Watch your rendered 9:16 Shorts and AI drafts",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MintGreenPrimary)
                }
            } else if (videos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎬", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No videos rendered yet", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Use the Studio to compile your first short.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(videos) { vid ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color(0x0C000000))
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .clickable { playingVideo = vid }
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MintGreenSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = MintGreenDark,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        vid.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        "${vid.durationSeconds}s • ${vid.renderStatus.replaceFirstChar { it.uppercase() }}",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                // Watch Pill Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MintGreenLight)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("▶", fontSize = 10.sp, color = MintGreenDark)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Watch", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MintGreenDark)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // In-App 9:16 Shorts Video Player Overlay (Full Screen Opaque)
        AnimatedVisibility(
            visible = playingVideo != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.fillMaxSize()
        ) {
            playingVideo?.let { vid ->
                val fullVideoUrl = if (vid.videoUrl.startsWith("http")) vid.videoUrl else "${ApiClient.getHost()}${vid.videoUrl}"
                var isPlaying by remember { mutableStateOf(true) }
                var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Video View
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.parse(fullVideoUrl))
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                    isPlaying = true
                                }
                                setOnCompletionListener {
                                    start()
                                }
                                videoViewRef = this
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                videoViewRef?.let { vv ->
                                    if (vv.isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                }
                            }
                    )

                    // Play / Pause Indicator in Center when paused
                    if (!isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    // Top Controls Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xEE000000), Color.Transparent)
                                )
                            )
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { playingVideo = null },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x66000000))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }

                            Text(
                                text = "9:16 Short Preview",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )

                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(fullVideoUrl), "video/mp4")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Watch Video"))
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x66000000))
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open External", tint = Color.White)
                            }
                        }
                    }

                    // Bottom Scrim with Details & Publish Action
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xEE000000))
                                )
                            )
                            .padding(20.dp)
                            .navigationBarsPadding()
                            .align(Alignment.BottomCenter)
                    ) {
                        Column {
                            Text(
                                text = vid.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = vid.description,
                                color = Color(0xCCFFFFFF),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        playingVideo = null
                                        onNavigateToPublish(vid)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(22.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                                ) {
                                    Text("▶", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Publish to YouTube", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
