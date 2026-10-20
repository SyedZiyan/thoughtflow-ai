package com.thoughtflow.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.thoughtflow.ai.data.model.GenerateThoughtRequest
import com.thoughtflow.ai.data.model.ThoughtResponse
import com.thoughtflow.ai.data.repository.ThoughtFlowRepository
import com.thoughtflow.ai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStudio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ThoughtFlowRepository() }

    val themes = listOf("motivation", "discipline", "consistency", "resilience", "mindfulness", "life lessons")
    val tones = listOf("inspiring", "stoic", "hard-hitting", "philosophical")

    var selectedTheme by remember { mutableStateOf(themes[0]) }
    var selectedTone by remember { mutableStateOf(tones[0]) }
    var customPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedThought by remember { mutableStateOf<ThoughtResponse?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Thought Generator", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Theme selector chips
            item {
                Text("Select Theme", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(themes) { th ->
                        val isSelected = selectedTheme == th
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MintGreenPrimary else Color.White)
                                .clickable { selectedTheme = th }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = th.replaceFirstChar { it.uppercase() },
                                color = if (isSelected) Color.White else TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Tone selector chips
            item {
                Text("Select Tone", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tones) { t ->
                        val isSelected = selectedTone == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) CyanFab else Color.White)
                                .clickable { selectedTone = t }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = t.replaceFirstChar { it.uppercase() },
                                color = if (isSelected) Color.White else TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Custom prompt text field
            item {
                Text("Custom Guidance (Optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    placeholder = { Text("e.g. Focus on morning discipline and overcoming procrastination...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = MintGreenPrimary,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    minLines = 3
                )
            }

            // Generate Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(12.dp, RoundedCornerShape(28.dp), spotColor = MintGreenPrimary)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MintGreenPrimary)
                        .clickable(enabled = !isGenerating) {
                            isGenerating = true
                            scope.launch {
                                repository.generateThought(
                                    GenerateThoughtRequest(
                                        theme = selectedTheme,
                                        tone = selectedTone,
                                        customPrompt = customPrompt.ifBlank { null }
                                    )
                                ).collect { res ->
                                    isGenerating = false
                                    res.onSuccess {
                                        generatedThought = it
                                        Toast.makeText(context, "Thought Generated!", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generate Original Thought",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Generated Result Card
            generatedThought?.let { th ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color(0x10000000))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "“${th.text}”", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Title: ${th.title}", fontSize = 12.sp, color = TextSecondary)
                            Button(
                                onClick = { onNavigateToStudio(th.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanFab),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open in Video Studio (9:16)")
                            }
                        }
                    }
                }
            }
        }
    }
}
