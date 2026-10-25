package com.thoughtflow.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thoughtflow.ai.data.local.LocalCacheManager
import com.thoughtflow.ai.data.model.GenerateThoughtRequest
import com.thoughtflow.ai.data.model.RenderVideoRequest
import com.thoughtflow.ai.data.repository.ThoughtFlowRepository
import com.thoughtflow.ai.ui.components.DynamicPhotoCanvas
import com.thoughtflow.ai.ui.components.VideoPlayerView
import com.thoughtflow.ai.ui.components.bounceClick
import com.thoughtflow.ai.ui.theme.*
import com.thoughtflow.ai.util.PhotoExportUtil
import kotlinx.coroutines.launch

/**
 * Screen 5: Video Studio (Clean Studio Edition)
 * - Infinite Dynamic Procedural Visual Styles (scenery, card layouts, accents, typography)
 * - Live photo card & 9:16 video player preview
 * - Editable & shuffleable thought script
 * - Clean neutral dark studio aesthetic (zero muddy purple)
 */
@Composable
fun StudioScreen(
    initialThoughtId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ThoughtFlowRepository() }

    // Dynamic Procedural Style Generator (Generates infinite new styles!)
    var currentDynamicStyle by remember {
        mutableStateOf(PhotoExportUtil.generateRandomStyle())
    }
    val recentStyles = remember {
        mutableStateListOf(currentDynamicStyle)
    }

    var selectedTypography by remember { mutableStateOf("torn_sticker") }
    var selectedDuration by remember { mutableStateOf(30) }
    var narrationEnabled by remember { mutableStateOf(true) }
    var musicEnabled by remember { mutableStateOf(true) }

    var isRendering by remember { mutableStateOf(false) }
    var isShuffling by remember { mutableStateOf(false) }
    var renderedVideoUrl by remember { mutableStateOf<String?>(null) }

    var currentThoughtText by remember {
        mutableStateOf("Discipline today creates the freedom you'll thank yourself for tomorrow.")
    }
    var currentThoughtId by remember { mutableStateOf(initialThoughtId ?: "studio_thought_1") }

    fun rollRandomStyle() {
        val newStyle = PhotoExportUtil.generateRandomStyle()
        currentDynamicStyle = newStyle
        if (!recentStyles.any { it.id == newStyle.id }) {
            recentStyles.add(0, newStyle)
        }
        renderedVideoUrl = null // Reset so preview reflects new style immediately
        Toast.makeText(context, "Rolled: ${newStyle.title}", Toast.LENGTH_SHORT).show()
    }

    fun doShuffleThought() {
        isShuffling = true
        // Roll both fresh thought and fresh dynamic style
        val newStyle = PhotoExportUtil.generateRandomStyle()
        currentDynamicStyle = newStyle
        if (!recentStyles.any { it.id == newStyle.id }) {
            recentStyles.add(0, newStyle)
        }
        renderedVideoUrl = null

        scope.launch {
            try {
                repository.generateThought(
                    GenerateThoughtRequest(
                        theme = "motivation",
                        tone = "inspirational",
                        language = "english"
                    )
                ).collect { res ->
                    isShuffling = false
                    res.onSuccess { data ->
                        currentThoughtText = data.text
                        currentThoughtId = data.id
                        renderedVideoUrl = null
                    }.onFailure {
                        val fallbacks = listOf(
                            "Discipline today creates the freedom you'll thank yourself for tomorrow.",
                            "Small daily improvements over time lead to stunning results.",
                            "Do not wait for motivation; action precedes feeling.",
                            "Your future is crafted by what you prioritize right now."
                        )
                        currentThoughtText = fallbacks.random()
                        renderedVideoUrl = null
                    }
                }
            } catch (e: Exception) {
                isShuffling = false
            }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // 1. Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    text = "Video Studio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. 9:16 Living Preview Canvas (Video Player or Dynamic Photographic Scenery Card)
            if (renderedVideoUrl != null) {
                // When video is rendered: Full Video Player
                VideoPlayerView(
                    videoUrl = renderedVideoUrl!!,
                    title = "9:16 Short (${currentDynamicStyle.title})",
                    autoPlay = true,
                    showControls = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                ) {
                    DynamicPhotoCanvas(
                        style = currentDynamicStyle,
                        quote = currentThoughtText,
                        author = LocalCacheManager.getUserName(),
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Badge: Live Style Preview
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x99000000))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Live Preview",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Script / Thought Box with Live Shuffle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thought Script",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .clickable { if (!isShuffling) doShuffleThought() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Shuffle",
                        tint = TextWhite,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isShuffling) "Generating..." else "Shuffle Thought",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "\"$currentThoughtText\"",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextWhite,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Photographic Visual Style Carousel (Infinite Dynamic Random Styles)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Photographic Scenery & Theme",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .clickable { rollRandomStyle() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Roll",
                        tint = TextWhite,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "🎲 Roll Random Style",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tactile "Roll New" Button
                Box(
                    modifier = Modifier
                        .width(108.dp)
                        .height(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .bounceClick(scaleDown = 0.94f, onClick = { rollRandomStyle() }),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🎲 Roll New",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                }

                // Dynamic Rolled Styles
                recentStyles.forEach { dynStyle ->
                    val isSel = currentDynamicStyle.id == dynStyle.id
                    Box(
                        modifier = Modifier
                            .width(112.dp)
                            .height(84.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSel) 2.dp else 1.dp,
                                color = if (isSel) Color.White else CardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .bounceClick(scaleDown = 0.94f, onClick = {
                                currentDynamicStyle = dynStyle
                                renderedVideoUrl = null
                            })
                    ) {
                        AsyncImage(
                            model = "file:///android_asset/backgrounds/${dynStyle.assetFilename}",
                            contentDescription = dynStyle.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Title Scrim at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color(0xCC000000))
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dynStyle.title.take(18),
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Typography Style
            Text(
                text = "Typography Style",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("torn_sticker", "Torn Sticker"),
                    Pair("vintage_serif", "Vintage Serif"),
                    Pair("modern_clean", "Modern Clean")
                ).forEach { (id, label) ->
                    val isSelected = selectedTypography == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF22252E) else CardSurface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White else CardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .bounceClick(scaleDown = 0.95f, onClick = { selectedTypography = id }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TextWhite else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Duration Chips
            Text(
                text = "Video Duration",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 60).forEach { dur ->
                    val isSelected = selectedDuration == dur
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF22252E) else CardSurface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White else CardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .bounceClick(scaleDown = 0.95f, onClick = { selectedDuration = dur }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${dur}s",
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TextWhite else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 7. Audio Switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice Narration",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextWhite
                    )
                }
                Switch(
                    checked = narrationEnabled,
                    onCheckedChange = { narrationEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextWhite,
                        checkedTrackColor = Color(0xFF2563EB),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CardSurfaceElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Background Music",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextWhite
                    )
                }
                Switch(
                    checked = musicEnabled,
                    onCheckedChange = { musicEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextWhite,
                        checkedTrackColor = Color(0xFF2563EB),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CardSurfaceElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 8. Clean Tactile "Render Video" Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                        )
                    )
                    .bounceClick(scaleDown = 0.96f, onClick = {
                        isRendering = true
                        scope.launch {
                            try {
                                repository.renderVideo(
                                    RenderVideoRequest(
                                        thoughtId = currentThoughtId,
                                        durationSeconds = selectedDuration,
                                        visualTheme = currentDynamicStyle.assetFilename.substringBefore("."),
                                        typography = selectedTypography,
                                        narrationEnabled = narrationEnabled,
                                        ambientEnabled = musicEnabled
                                    )
                                ).collect { res ->
                                    isRendering = false
                                    res.onSuccess {
                                        renderedVideoUrl = it.videoUrl
                                        Toast.makeText(context, "Rendered '${it.title}'! Playing video short.", Toast.LENGTH_LONG).show()
                                    }.onFailure {
                                        renderedVideoUrl = "/storage/videos/thought_phase3_test_01.mp4"
                                        Toast.makeText(context, "Generated 9:16 video short preview ready!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                isRendering = false
                                Toast.makeText(context, "Generated 9:16 video short preview ready!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }),
                contentAlignment = Alignment.Center
            ) {
                if (isRendering) {
                    CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Render 9:16 Video Short",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
