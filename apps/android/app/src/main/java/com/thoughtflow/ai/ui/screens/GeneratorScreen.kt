package com.thoughtflow.ai.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thoughtflow.ai.data.local.LocalCacheManager
import com.thoughtflow.ai.data.model.GenerateThoughtRequest
import com.thoughtflow.ai.data.repository.ThoughtFlowRepository
import com.thoughtflow.ai.ui.components.bounceClick
import com.thoughtflow.ai.ui.components.DynamicPhotoCanvas
import com.thoughtflow.ai.ui.theme.*
import com.thoughtflow.ai.util.PhotoExportUtil
import kotlinx.coroutines.launch

/**
 * Screen 4: Content Generator (Video Shorts & Photo Quote Posts)
 * Lets creators choose between rendering a 9:16 Video Short or exporting a high-res Photo Quote Card.
 */
@Composable
fun GeneratorScreen(
    initialMode: String = "video", // "video" or "photo"
    onNavigateBack: () -> Unit,
    onNavigateToStudio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ThoughtFlowRepository() }

    var currentMode by remember { mutableStateOf(if (initialMode.lowercase() == "photo") "photo" else "video") }

    val themes = listOf("Motivation", "Discipline", "Success", "Mindfulness", "Resilience", "Consistency")
    val tones = listOf("Inspirational", "Stoic", "Direct", "Philosophical", "Poetic")
    val languages = listOf("English", "Hindi", "Spanish", "French", "German")

    var selectedTheme by remember { mutableStateOf(themes[0]) }
    var selectedTone by remember { mutableStateOf(tones[0]) }
    var selectedLanguage by remember { mutableStateOf(languages[0]) }

    var themeExpanded by remember { mutableStateOf(false) }
    var toneExpanded by remember { mutableStateOf(false) }
    var langExpanded by remember { mutableStateOf(false) }

    var isFavorite by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    var currentThoughtText by remember {
        mutableStateOf("Discipline today creates the freedom you'll thank yourself for tomorrow.")
    }
    var currentThoughtId by remember { mutableStateOf("demo_thought_1") }

    // Photo-specific customizations matching Reference Images 2 & 3
    val photoAspectRatios = listOf("9:16", "1:1", "4:5")
    var selectedAspectRatio by remember { mutableStateOf("9:16") }

    // Dynamic Procedural Style Generator (Generates infinite new styles!)
    var currentDynamicStyle by remember {
        mutableStateOf(PhotoExportUtil.generateRandomStyle())
    }
    val recentStyles = remember {
        mutableStateListOf(currentDynamicStyle)
    }

    fun rollRandomStyle() {
        val newStyle = PhotoExportUtil.generateRandomStyle()
        currentDynamicStyle = newStyle
        if (!recentStyles.any { it.id == newStyle.id }) {
            recentStyles.add(0, newStyle)
        }
        Toast.makeText(context, "Rolled: ${newStyle.title}", Toast.LENGTH_SHORT).show()
    }

    fun doGenerate() {
        isGenerating = true
        // Roll a fresh random style automatically on every generate!
        val newStyle = PhotoExportUtil.generateRandomStyle()
        currentDynamicStyle = newStyle
        if (!recentStyles.any { it.id == newStyle.id }) {
            recentStyles.add(0, newStyle)
        }
        scope.launch {
            try {
                repository.generateThought(
                    GenerateThoughtRequest(
                        theme = selectedTheme.lowercase(),
                        tone = selectedTone.lowercase(),
                        language = selectedLanguage.lowercase()
                    )
                ).collect { res ->
                    isGenerating = false
                    res.onSuccess { data ->
                        currentThoughtText = data.text
                        currentThoughtId = data.id
                        isFavorite = false
                    }.onFailure {
                        val fallbacks = listOf(
                            "Discipline today creates the freedom you'll thank yourself for tomorrow.",
                            "Small daily improvements over time lead to stunning results.",
                            "Do not wait for motivation; action precedes feeling.",
                            "Your future is crafted by what you prioritize right now."
                        )
                        currentThoughtText = fallbacks.random()
                        Toast.makeText(context, "Generated offline thought", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                isGenerating = false
                currentThoughtText = "Discipline today creates the freedom you'll thank yourself for tomorrow."
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
                    text = "Content Generator",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Segmented Format Selector: [ 🎬 Video Short ] | [ 📸 Photo Post ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                // Video Option
                val isVideo = currentMode == "video"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isVideo) Color(0xFF22252E)
                            else Color.Transparent
                        )
                        .border(
                            1.dp,
                            if (isVideo) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .bounceClick(scaleDown = 0.96f, onClick = { currentMode = "video" }),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (isVideo) TextWhite else TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Video Short",
                            fontSize = 13.sp,
                            fontWeight = if (isVideo) FontWeight.Bold else FontWeight.Medium,
                            color = if (isVideo) TextWhite else TextSecondary
                        )
                    }
                }

                // Photo Option
                val isPhoto = currentMode == "photo"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isPhoto) Color(0xFF22252E)
                            else Color.Transparent
                        )
                        .border(
                            1.dp,
                            if (isPhoto) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .bounceClick(scaleDown = 0.96f, onClick = { currentMode = "photo" }),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = if (isPhoto) TextWhite else TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Photo Post",
                            fontSize = 13.sp,
                            fontWeight = if (isPhoto) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPhoto) TextWhite else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Theme Dropdown
            Text(
                text = "Theme",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            DropdownSelector(
                value = selectedTheme,
                expanded = themeExpanded,
                onExpandedChange = { themeExpanded = it },
                items = themes,
                onSelect = { selectedTheme = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Tone & Language in Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tone",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DropdownSelector(
                        value = selectedTone,
                        expanded = toneExpanded,
                        onExpandedChange = { toneExpanded = it },
                        items = tones,
                        onSelect = { selectedTone = it }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Language",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DropdownSelector(
                        value = selectedLanguage,
                        expanded = langExpanded,
                        onExpandedChange = { langExpanded = it },
                        items = languages,
                        onSelect = { selectedLanguage = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 5. "Generate Thought" Tactile Button (Clean Studio Blue)
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
                    .bounceClick(scaleDown = 0.96f, onClick = { if (!isGenerating) doGenerate() }),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (currentMode == "photo") Icons.Default.Image else Icons.Default.Videocam,
                            contentDescription = null,
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentMode == "photo") "Generate Quote Photo" else "Generate Quote Video",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 6. MODE-SPECIFIC CANVAS & ACTIONS
            if (currentMode == "photo") {
                // ==================== PHOTO MODE ====================
                Text(
                    text = "Photo Aspect Ratio",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("9:16 (Story)", "1:1 (Square)", "4:5 (Feed)").forEach { ratioLabel ->
                        val code = ratioLabel.substringBefore(" ")
                        val isSel = selectedAspectRatio == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) Color(0xFF22252E) else CardSurface)
                                .border(1.dp, if (isSel) Color.White.copy(alpha = 0.2f) else CardBorder, RoundedCornerShape(10.dp))
                                .bounceClick(scaleDown = 0.95f, onClick = { selectedAspectRatio = code }),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ratioLabel,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) TextWhite else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Visual Theme Header with "🎲 Roll Random Style" Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visual Theme & Scenery",
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

                // Horizontal Carousel of Dynamic Rolled Styles
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
                                .bounceClick(scaleDown = 0.94f, onClick = { currentDynamicStyle = dynStyle })
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

                val previewHeight = when (selectedAspectRatio) {
                    "1:1" -> 320.dp
                    "4:5" -> 390.dp
                    else -> 460.dp // 9:16
                }

                // Live Dynamic Procedural Canvas Preview
                DynamicPhotoCanvas(
                    style = currentDynamicStyle,
                    quote = currentThoughtText,
                    author = LocalCacheManager.getUserName(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(previewHeight)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Save Photo to Gallery & Share Photo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Save to Gallery: Clean Studio Blue
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                )
                            )
                            .bounceClick(scaleDown = 0.95f, onClick = {
                                val bitmap = PhotoExportUtil.createQuoteBitmap(
                                    context = context,
                                    quote = currentThoughtText,
                                    author = LocalCacheManager.getUserName(),
                                    aspectRatio = selectedAspectRatio,
                                    dynamicStyle = currentDynamicStyle
                                )
                                val uri = PhotoExportUtil.saveBitmapToGallery(context, bitmap)
                                if (uri != null) {
                                    Toast.makeText(context, "Saved to Gallery (Pictures/ThoughtFlow)!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Could not save photo to Gallery", Toast.LENGTH_SHORT).show()
                                }
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Gallery", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                    }

                    // Share Photo: Clean Studio Graphite
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardSurface)
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .bounceClick(scaleDown = 0.95f, onClick = {
                                val bitmap = PhotoExportUtil.createQuoteBitmap(
                                    context = context,
                                    quote = currentThoughtText,
                                    author = LocalCacheManager.getUserName(),
                                    aspectRatio = selectedAspectRatio,
                                    dynamicStyle = currentDynamicStyle
                                )
                                PhotoExportUtil.shareBitmap(context, bitmap, currentThoughtText)
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Photo", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = TextWhite)
                        }
                    }
                }
            } else {
                // ==================== VIDEO MODE ====================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Generated Thought Script",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )

                            IconButton(
                                onClick = { isFavorite = !isFavorite },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) StatusRed else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "\"$currentThoughtText\"",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 26.sp,
                            color = TextWhite
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Regenerate Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardSurface)
                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                    .bounceClick(scaleDown = 0.95f, onClick = { doGenerate() }),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Regenerate",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextWhite
                                    )
                                }
                            }

                            // Render Video Short Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                        )
                                    )
                                    .bounceClick(scaleDown = 0.95f, onClick = { onNavigateToStudio(currentThoughtId) }),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = TextWhite, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Render Video",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun DropdownSelector(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<String>,
    onSelect: (String) -> Unit
) {
    val specularBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.03f)
        )
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CardSurface)
                .border(1.dp, specularBorder, RoundedCornerShape(14.dp))
                .bounceClick(scaleDown = 0.98f, onClick = { onExpandedChange(!expanded) })
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .background(CardSurfaceElevated)
                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            color = if (item == value) PurplePrimary else TextWhite,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        onSelect(item)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}
