package com.thoughtflow.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtflow.ai.data.model.VideoResponse
import com.thoughtflow.ai.data.repository.ThoughtFlowRepository
import com.thoughtflow.ai.ui.theme.*

@Composable
fun LibraryScreen(
    onVideoSelected: (VideoResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = remember { ThoughtFlowRepository() }
    var videos by remember { mutableStateOf<List<VideoResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repository.getVideos().collect { res ->
            isLoading = false
            res.onSuccess { videos = it }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text("Content Library", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Browse your rendered 9:16 Shorts and AI drafts", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

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
                            .clickable { onVideoSelected(vid) }
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
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MintGreenDark, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(vid.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                                Text("${vid.durationSeconds}s • ${vid.renderStatus.replaceFirstChar { it.uppercase() }}", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                            }
                            Text(
                                text = vid.uploadStatus.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (vid.uploadStatus == "published") MintGreenDark else CyanFab,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (vid.uploadStatus == "published") MintGreenSoft else Color(0xFFE0F2FE))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
