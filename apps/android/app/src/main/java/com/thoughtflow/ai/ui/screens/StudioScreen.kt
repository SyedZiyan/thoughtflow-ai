package com.thoughtflow.ai.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
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
import com.thoughtflow.ai.data.model.RenderVideoRequest
import com.thoughtflow.ai.data.repository.ThoughtFlowRepository
import com.thoughtflow.ai.ui.theme.*
import kotlinx.coroutines.launch

data class ThemeOption(val id: String, val name: String, val tag: String, val color: Color, val defaultTypo: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    initialThoughtId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ThoughtFlowRepository() }

    val themes = listOf(
        ThemeOption("cozy_balcony", "Cozy Balcony", "Ref 1", Color(0xFFF5C276), "poetic_script"),
        ThemeOption("mountain_summit", "Mountain Summit", "Ref 2", Color(0xFF60A5FA), "bold_impact"),
        ThemeOption("misty_forest", "Misty Forest", "Photo", Color(0xFF34D399), "bold_impact"),
        ThemeOption("ocean_sunset", "Ocean Twilight", "Photo", Color(0xFFFB923C), "poetic_script"),
        ThemeOption("starry_night", "Starry Night", "Photo", Color(0xFFA855F7), "poetic_script")
    )

    var selectedTheme by remember { mutableStateOf(themes[0]) }
    var selectedTypo by remember { mutableStateOf("poetic_script") }
    var durationSeconds by remember { mutableStateOf(15) }
    var narrationEnabled by remember { mutableStateOf(false) }
    var ambientEnabled by remember { mutableStateOf(true) }
    var isRendering by remember { mutableStateOf(false) }
    var renderedResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Studio (9:16)", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            // Visual Theme Selection Card
            item {
                Text(
                    text = "1. Photographic Visual Theme",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(themes) { th ->
                        val isSelected = selectedTheme.id == th.id
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(95.dp)
                                .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0x10000000))
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MintGreenPrimary else BorderSubtle,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    selectedTheme = th
                                    selectedTypo = th.defaultTypo
                                }
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(th.color)
                                    )
                                    Text(
                                        text = th.tag,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MintGreenDark,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MintGreenSoft)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = th.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Typography Selection
            item {
                Text(
                    text = "2. Typography Style",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "poetic_script" to "Poetic Script (Gold Cursive)",
                        "bold_impact" to "Bold Impact (Thought of Day)"
                    ).forEach { (id, label) ->
                        val isSelected = selectedTypo == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MintGreenSoft else Color.White)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MintGreenPrimary else BorderSubtle,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedTypo = id }
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MintGreenDark else TextPrimary
                            )
                        }
                    }
                }
            }

            // Audio & Duration Settings Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color(0x0A000000))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "3. Duration & Audio Engine",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        // Duration pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(10, 15, 20, 30).forEach { dur ->
                                val isSelected = durationSeconds == dur
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MintGreenPrimary else Color(0xFFF1F5F9))
                                        .clickable { durationSeconds = dur }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${dur}s",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }

                        // Narration toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("AI Voice Narration", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("High-fidelity Edge-TTS voiceover", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = narrationEnabled,
                                onCheckedChange = { narrationEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MintGreenPrimary)
                            )
                        }

                        // Ambient track toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Ambient Music", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("Binaural motivational backdrop", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = ambientEnabled,
                                onCheckedChange = { ambientEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MintGreenPrimary)
                            )
                        }
                    }
                }
            }

            // Render Button Action
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(12.dp, RoundedCornerShape(29.dp), spotColor = MintGreenPrimary)
                        .clip(RoundedCornerShape(29.dp))
                        .background(MintGreenPrimary)
                        .clickable(enabled = !isRendering) {
                            val thoughtId = initialThoughtId ?: "default"
                            isRendering = true
                            scope.launch {
                                repository.renderVideo(
                                    RenderVideoRequest(
                                        thoughtId = thoughtId,
                                        durationSeconds = durationSeconds,
                                        visualTheme = selectedTheme.id,
                                        typography = selectedTypo,
                                        narrationEnabled = narrationEnabled,
                                        ambientEnabled = ambientEnabled
                                    )
                                ).collect { res ->
                                    isRendering = false
                                    res.onSuccess {
                                        renderedResult = it.videoUrl
                                        Toast.makeText(context, "Video Rendered: ${it.title}", Toast.LENGTH_LONG).show()
                                    }.onFailure {
                                        Toast.makeText(context, "Render error: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRendering) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compile & Render 9:16 Video",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
