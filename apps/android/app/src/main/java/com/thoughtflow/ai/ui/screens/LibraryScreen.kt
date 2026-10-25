package com.thoughtflow.ai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.thoughtflow.ai.data.model.VideoResponse
import com.thoughtflow.ai.data.repository.ThoughtFlowRepository
import com.thoughtflow.ai.ui.components.CinematicBackdrop
import com.thoughtflow.ai.ui.components.VideoPlayerView
import com.thoughtflow.ai.ui.components.bounceClick
import com.thoughtflow.ai.ui.components.resolveMediaUrl
import com.thoughtflow.ai.ui.theme.*

data class LibraryItemData(
    val id: String,
    val title: String,
    val theme: String,
    val status: String,
    val statusText: String,
    val statusColor: Color,
    val youtubeViews: String? = null,
    val videoUrl: String? = null
)

/**
 * Screen 6: Content Library (Elite Polish Edition)
 * - Filter pills with spring bounce: All | Drafts | Rendered | Published
 * - Specular Glass cards with living cinematic thumbnails
 * - YouTube view count badge
 * - Fullscreen 9:16 living video playback preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToPublish: (VideoResponse) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ThoughtFlowRepository() }

    val filterOptions = listOf("All", "Drafts", "Rendered", "Published")
    var selectedFilter by remember { mutableStateOf("All") }

    var backendVideos by remember { mutableStateOf<List<VideoResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var playingItem by remember { mutableStateOf<LibraryItemData?>(null) }
    var selectedActionItem by remember { mutableStateOf<LibraryItemData?>(null) }

    val specularBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            Color.White.copy(alpha = 0.03f)
        )
    )

    LaunchedEffect(Unit) {
        isLoading = true
        repository.getVideos().collect { res ->
            isLoading = false
            res.onSuccess { backendVideos = it }
        }
    }

    val defaultItems = listOf(
        LibraryItemData(
            id = "1",
            title = "Small steps create extraordinary change.",
            theme = "Motivation",
            status = "published",
            statusText = "✓ Published • 2 days ago",
            statusColor = StatusGreen,
            youtubeViews = "12K views",
            videoUrl = "/storage/videos/thought_phase3_test_01.mp4"
        ),
        LibraryItemData(
            id = "2",
            title = "A calm mind is a powerful mind.",
            theme = "Mindfulness",
            status = "scheduled",
            statusText = "⏰ Scheduled • Tomorrow",
            statusColor = StatusAmber,
            videoUrl = "/storage/videos/thought_test_sample_01.mp4"
        ),
        LibraryItemData(
            id = "3",
            title = "Discipline today creates freedom tomorrow.",
            theme = "Discipline",
            status = "rendered",
            statusText = "⚙ Rendered • 4 hours ago",
            statusColor = StatusBlue,
            videoUrl = "/storage/videos/thought_preview_demo_001.mp4"
        ),
        LibraryItemData(
            id = "4",
            title = "Your only limit is your mind.",
            theme = "Success",
            status = "draft",
            statusText = "• Draft • 1 day ago",
            statusColor = TextMuted,
            videoUrl = "/storage/videos/thought_phase3_test_01.mp4"
        ),
        LibraryItemData(
            id = "5",
            title = "Consistency turns ordinary into excellence.",
            theme = "Growth",
            status = "failed",
            statusText = "• Failed • 3 days ago",
            statusColor = StatusRed,
            videoUrl = "/storage/videos/thought_test_sample_01.mp4"
        )
    )

    val displayItems = remember(backendVideos, selectedFilter) {
        val list = if (backendVideos.isNotEmpty()) {
            backendVideos.map { v ->
                val vStatus = when {
                    v.uploadStatus.equals("published", ignoreCase = true) -> "published"
                    v.uploadStatus.equals("scheduled", ignoreCase = true) -> "scheduled"
                    v.renderStatus.equals("completed", ignoreCase = true) || v.renderStatus.equals("rendered", ignoreCase = true) -> "rendered"
                    v.renderStatus.equals("failed", ignoreCase = true) -> "failed"
                    else -> "draft"
                }
                val (stText, stCol) = when (vStatus) {
                    "published" -> "✓ Published" to StatusGreen
                    "scheduled" -> "⏰ Scheduled" to StatusAmber
                    "rendered" -> "⚙ Rendered" to StatusBlue
                    "failed" -> "• Failed" to StatusRed
                    else -> "• Draft" to TextMuted
                }
                LibraryItemData(
                    id = v.id,
                    title = v.title,
                    theme = "Inspiration",
                    status = vStatus,
                    statusText = stText,
                    statusColor = stCol,
                    youtubeViews = if (vStatus == "published") "8.4K views" else null,
                    videoUrl = v.videoUrl
                )
            }
        } else {
            defaultItems
        }

        when (selectedFilter) {
            "Drafts" -> list.filter { it.status == "draft" }
            "Rendered" -> list.filter { it.status == "rendered" }
            "Published" -> list.filter { it.status == "published" || it.status == "scheduled" }
            else -> list
        }
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
            // 1. Header with Back Button
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
                    text = "Content Library",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            // 2. Filter Pills Row with Tactile Bounce
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filterOptions) { filter ->
                    val isSelected = selectedFilter == filter
                    val pillBorder = if (isSelected) Brush.linearGradient(listOf(PurplePrimary, PurpleAccent)) else specularBorder
                    Box(
                        modifier = Modifier
                            .shadow(if (isSelected) 8.dp else 0.dp, RoundedCornerShape(20.dp), spotColor = PurplePrimary)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) PurplePrimary else CardSurface
                            )
                            .border(1.dp, pillBorder, RoundedCornerShape(20.dp))
                            .bounceClick(scaleDown = 0.92f, onClick = { selectedFilter = filter })
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) TextWhite else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Videos Feed List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayItems) { item ->
                    LibraryCardItem(
                        item = item,
                        onClick = { playingItem = item },
                        onOptionsClick = { selectedActionItem = item }
                    )
                }
            }
        }

        // Fullscreen 9:16 Video Player Dialog
        AnimatedVisibility(
            visible = playingItem != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            playingItem?.let { current ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xF0000000))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color(0x667C3AED))
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1F173B), Color(0xFF130E26))
                                )
                            )
                            .border(1.dp, specularBorder, RoundedCornerShape(24.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = current.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { playingItem = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real Video Player View
                        VideoPlayerView(
                            videoUrl = current.videoUrl ?: "/storage/videos/thought_phase3_test_01.mp4",
                            title = current.title,
                            autoPlay = true,
                            showControls = true,
                            onClose = { playingItem = null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Share Video Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0x4038BDF8))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                        )
                                    )
                                    .bounceClick(scaleDown = 0.96f, onClick = {
                                        val shareUrl = resolveMediaUrl(current.videoUrl)
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Watch '${current.title}' created on ThoughtFlow AI: $shareUrl")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Video Short"))
                                    }),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share Video", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                                }
                            }

                            // Done Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0x667C3AED))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(PurplePrimary, PurpleDark)
                                        )
                                    )
                                    .bounceClick(scaleDown = 0.96f, onClick = { playingItem = null }),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Done", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                            }
                        }
                    }
                }
            }
        }

        // Action Options Bottom Sheet
        if (selectedActionItem != null) {
            val act = selectedActionItem!!
            ModalBottomSheet(
                onDismissRequest = { selectedActionItem = null },
                containerColor = Color(0xFF15102A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = act.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ListItem(
                        headlineContent = { Text("Play Video Short", color = TextWhite, fontWeight = FontWeight.Medium) },
                        leadingContent = { Icon(Icons.Default.PlayCircle, contentDescription = null, tint = PurpleAccent) },
                        modifier = Modifier.clickable {
                            val target = act
                            selectedActionItem = null
                            playingItem = target
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        headlineContent = { Text("Share Video Link", color = TextWhite, fontWeight = FontWeight.Medium) },
                        leadingContent = { Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF38BDF8)) },
                        modifier = Modifier.clickable {
                            selectedActionItem = null
                            val shareUrl = resolveMediaUrl(act.videoUrl)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Watch '${act.title}' on ThoughtFlow AI: $shareUrl")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Video Short"))
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        headlineContent = { Text("Copy Quote Text", color = TextWhite, fontWeight = FontWeight.Medium) },
                        leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = StatusAmber) },
                        modifier = Modifier.clickable {
                            selectedActionItem = null
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Thought Quote", act.title))
                            Toast.makeText(context, "Quote copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun LibraryCardItem(
    item: LibraryItemData,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
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
            .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = Color(0x20000000))
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF191330), Color(0xFF110D24))
                )
            )
            .border(1.dp, specularBorder, RoundedCornerShape(18.dp))
            .bounceClick(scaleDown = 0.97f, onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Living Thumbnail with subtle ambient motion
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                CinematicBackdrop(enableKenBurns = false, dimAlpha = 0.25f, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.theme,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = item.statusColor
                )
            }

            // Right Badges & More icon
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (item.youtubeViews != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33EF4444))
                            .border(0.8.dp, Color(0x66EF4444), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "▶ ${item.youtubeViews}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B6B),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
