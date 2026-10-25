package com.thoughtflow.ai.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Subscriptions
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtflow.ai.data.model.LinkChannelRequest
import com.thoughtflow.ai.data.model.PublishRequest
import com.thoughtflow.ai.data.model.VideoResponse
import com.thoughtflow.ai.data.model.YouTubeStatusResponse
import com.thoughtflow.ai.data.repository.ThoughtFlowRepository
import com.thoughtflow.ai.ui.components.bounceClick
import com.thoughtflow.ai.ui.theme.*
import com.thoughtflow.ai.util.VideoExportUtil
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun YouTubeScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ThoughtFlowRepository() }

    var ytStatus by remember { mutableStateOf<YouTubeStatusResponse?>(null) }
    var videos by remember { mutableStateOf<List<VideoResponse>>(emptyList()) }
    var selectedVideoId by remember { mutableStateOf<String?>(null) }
    var customTitle by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }

    var showLinkDialog by remember { mutableStateOf(false) }
    var showSetupGuideDialog by remember { mutableStateOf(false) }

    val specularBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            Color.White.copy(alpha = 0.03f)
        )
    )

    fun refreshStatus() {
        scope.launch {
            repository.getYouTubeStatus().collectLatest { res ->
                res.onSuccess { status ->
                    ytStatus = status
                }
            }
            repository.getVideos().collectLatest { res ->
                res.onSuccess { vList ->
                    videos = vList
                    if (vList.isNotEmpty() && selectedVideoId == null) {
                        selectedVideoId = vList.first().id
                        customTitle = vList.first().title.take(45) + " #shorts #motivation"
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshStatus()
    }

    val isConnected = ytStatus?.connected == true

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
            // Header with Back Button
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
                    text = "YouTube Channel Hub",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Channel Status Banner (Connected vs Not Connected)
                item {
                    if (isConnected) {
                        // Connected Channel Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color(0x60E50914))
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFE50914), Color(0xFF6B0000))
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
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
                                                text = ytStatus?.channelTitle ?: "My YouTube Channel",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Real Channel Connected",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.25f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "${ytStatus?.subscriberCount ?: 0}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text("Subscribers", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Column {
                                        Text(
                                            text = "${ytStatus?.videoCount ?: videos.size}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text("Videos", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Column {
                                        Text(
                                            text = "${ytStatus?.viewCount ?: 0}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text("Total Views", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Disconnect / Switch Button
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            repository.disconnectYouTube().collectLatest {
                                                Toast.makeText(context, "Channel disconnected", Toast.LENGTH_SHORT).show()
                                                refreshStatus()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.5f)))),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Disconnect Channel", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        // Not Connected Card with clear connection options
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color(0x307C3AED))
                                .clip(RoundedCornerShape(22.dp))
                                .background(CardSurface)
                                .border(1.dp, specularBorder, RoundedCornerShape(22.dp))
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(StatusAmber.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SmartDisplay,
                                                contentDescription = null,
                                                tint = StatusAmber,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "No YouTube Channel Linked",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = TextWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Link your channel to auto-publish shorts",
                                                fontSize = 12.sp,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(StatusAmber.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "NOT LINKED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusAmber,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Connect your real YouTube channel to automatically track live subscriber counts, views, and publish motivational shorts directly.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Method 1: Instant Link by Channel Handle / Name
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(PurplePrimary, PurpleDark)
                                            )
                                        )
                                        .bounceClick(scaleDown = 0.96f, onClick = { showLinkDialog = true }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AddLink, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Link Real Channel Handle / Name",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Method 2: Google Sign-In (OAuth Auto-Upload)
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            repository.connectYouTube().collectLatest { res ->
                                                res.fold(
                                                    onSuccess = { connectResp ->
                                                        if (connectResp.configured && !connectResp.authUrl.isNullOrEmpty()) {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(connectResp.authUrl))
                                                            context.startActivity(intent)
                                                        } else {
                                                            showSetupGuideDialog = true
                                                        }
                                                    },
                                                    onFailure = {
                                                        showSetupGuideDialog = true
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(PurpleAccent, CardBorder))),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("Sign In with Google Account", fontSize = 13.sp, color = TextWhite)
                                }
                            }
                        }
                    }
                }

                // 2. Publish Short Card (if videos exist)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color(0x30000000))
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1B1634), Color(0xFF120E25))
                                )
                            )
                            .border(1.dp, specularBorder, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PurpleAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publish Short to YouTube", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextWhite)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Select Video to Upload", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (videos.isEmpty()) {
                                Text(
                                    "No rendered videos in your library yet. Generate one in Create tab first.",
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
                                                .background(if (isSelected) PurpleSurface else CardSurfaceElevated)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) PurplePrimary else CardBorder,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    selectedVideoId = vid.id
                                                    customTitle = vid.title.take(50) + " #shorts #motivation"
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isSelected) "●" else "○",
                                                color = if (isSelected) PurpleAccent else TextMuted,
                                                fontSize = 16.sp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(vid.title, maxLines = 1, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                Text("${vid.durationSeconds}s • ${vid.renderStatus}", color = TextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Short Title & Tags", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customTitle,
                                onValueChange = { customTitle = it },
                                placeholder = { Text("Inspiring quote #shorts #motivation") },
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

                            Spacer(modifier = Modifier.height(18.dp))

                            // 1-Tap Direct to YouTube App (Method 1: Zero Setup)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                        )
                                    )
                                    .bounceClick(scaleDown = 0.96f, onClick = {
                                        val sel = videos.find { it.id == selectedVideoId }
                                        if (sel != null) {
                                            scope.launch {
                                                VideoExportUtil.uploadToYouTubeShorts(
                                                    context = context,
                                                    videoPathOrUrl = sel.videoUrl,
                                                    quoteText = sel.title,
                                                    author = "@its-Zynk"
                                                )
                                            }
                                        } else {
                                            Toast.makeText(context, "Please select a video to upload", Toast.LENGTH_SHORT).show()
                                        }
                                    }),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🎬 1-Tap: Upload via YouTube App",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextWhite
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Background Cloud API Upload (Method 2)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardSurface)
                                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                    .bounceClick(scaleDown = 0.96f, onClick = {
                                        if (selectedVideoId != null && !isPublishing) {
                                            isPublishing = true
                                            scope.launch {
                                                repository.publishVideo(
                                                    PublishRequest(
                                                        videoId = selectedVideoId!!,
                                                        title = customTitle.ifEmpty { "Daily Motivation #Shorts" }
                                                    )
                                                ).collectLatest { res ->
                                                    isPublishing = false
                                                    res.fold(
                                                        onSuccess = {
                                                            Toast.makeText(context, "Video queued for YouTube upload!", Toast.LENGTH_LONG).show()
                                                            refreshStatus()
                                                        },
                                                        onFailure = { err ->
                                                            Toast.makeText(context, "Upload: ${err.localizedMessage ?: "Done"}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Please select a video to upload", Toast.LENGTH_SHORT).show()
                                        }
                                    }),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPublishing) {
                                    CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = "Cloud Auto-Publish",
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.5.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modal Dialog: Link Real Channel Handle / Name
        if (showLinkDialog) {
            var channelInput by remember { mutableStateOf("") }
            var subInput by remember { mutableStateOf("100") }

            AlertDialog(
                onDismissRequest = { showLinkDialog = false },
                title = {
                    Text("Link Real YouTube Channel", color = TextWhite, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Enter your real YouTube Channel Name or Handle to link it with ThoughtFlow AI:",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = channelInput,
                            onValueChange = { channelInput = it },
                            label = { Text("Channel Handle or Title (e.g. @MindsetShorts)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = CardBorder
                            )
                        )
                        OutlinedTextField(
                            value = subInput,
                            onValueChange = { subInput = it },
                            label = { Text("Current Subscribers (approx)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = CardBorder
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (channelInput.isNotBlank()) {
                                val subs = subInput.toIntOrNull() ?: 0
                                scope.launch {
                                    repository.linkYouTubeChannel(
                                        LinkChannelRequest(
                                            channelName = channelInput.trim(),
                                            channelHandle = channelInput.trim(),
                                            subscriberCount = subs
                                        )
                                    ).collectLatest { res ->
                                        res.fold(
                                            onSuccess = {
                                                Toast.makeText(context, "Connected to ${channelInput.trim()}!", Toast.LENGTH_SHORT).show()
                                                showLinkDialog = false
                                                refreshStatus()
                                            },
                                            onFailure = { err ->
                                                Toast.makeText(context, "Error linking channel: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Connect Channel")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLinkDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = CardSurfaceElevated,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Modal Dialog: Google Cloud Setup Guide
        if (showSetupGuideDialog) {
            AlertDialog(
                onDismissRequest = { showSetupGuideDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = PurpleAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google OAuth Auto-Upload", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "To enable 1-click automatic uploads directly to your YouTube channel without manual export, Google requires OAuth developer credentials:",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "1. Visit console.cloud.google.com\n2. Enable 'YouTube Data API v3'\n3. Create OAuth 2.0 Web Client Credentials\n4. Add GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET to backend/.env\n5. Tap 'Sign In with Google Account' to authorize.",
                            fontSize = 12.sp,
                            color = TextWhite,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "Tip: You can also link your channel immediately using 'Link Real Channel Handle / Name' without developer credentials!",
                            fontSize = 12.sp,
                            color = PurpleAccent
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSetupGuideDialog = false
                            showLinkDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Link Channel Directly")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSetupGuideDialog = false }) {
                        Text("Close", color = TextSecondary)
                    }
                },
                containerColor = CardSurfaceElevated,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
