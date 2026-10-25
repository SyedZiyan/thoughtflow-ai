package com.thoughtflow.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.thoughtflow.ai.ui.components.bounceClick
import com.thoughtflow.ai.ui.theme.*

/**
 * Screen 8: Automation Settings (Elite Polish Edition)
 * - Enable Daily Automation toggle
 * - Auto Publish to YouTube toggle
 * - Generation Time & Publishing Time dropdowns
 * - Timezone selector (Asia/Kolkata)
 * - Theme Rotation chips with spring physics
 * - Video Duration, Add Narration, Daily Limit
 * - "Save Settings" tactile purple button
 */
@Composable
fun AutomationScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var dailyAutomationEnabled by remember { mutableStateOf(true) }
    var autoPublishEnabled by remember { mutableStateOf(true) }
    var generationTime by remember { mutableStateOf("8:00 AM") }
    var publishingTime by remember { mutableStateOf("8:00 AM") }
    var timezone by remember { mutableStateOf("Asia/Kolkata (IST)") }
    var videoDuration by remember { mutableStateOf("30 seconds") }
    var narrationEnabled by remember { mutableStateOf(true) }
    var dailyLimit by remember { mutableStateOf("1") }

    val activeThemes = remember {
        mutableStateListOf("Motivation", "Success", "Mindfulness")
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
                    text = "Automation Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Toggle 1: Enable Daily Automation
            SettingSwitchRow(
                title = "Enable Daily Automation",
                checked = dailyAutomationEnabled,
                onCheckedChange = { dailyAutomationEnabled = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Toggle 2: Auto Publish to YouTube
            SettingSwitchRow(
                title = "Auto Publish to YouTube",
                checked = autoPublishEnabled,
                onCheckedChange = { autoPublishEnabled = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Generation Time
            SettingDropdownRow(
                label = "Generation Time",
                value = generationTime,
                options = listOf("6:00 AM", "7:00 AM", "8:00 AM", "9:00 AM"),
                onSelect = { generationTime = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Publishing Time
            SettingDropdownRow(
                label = "Publishing Time",
                value = publishingTime,
                options = listOf("8:00 AM", "9:00 AM", "12:00 PM", "6:00 PM"),
                onSelect = { publishingTime = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Timezone
            SettingDropdownRow(
                label = "Timezone",
                value = timezone,
                options = listOf("Asia/Kolkata (IST)", "UTC", "America/New_York (EST)", "Europe/London (GMT)"),
                onSelect = { timezone = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 7. Theme Rotation Section
            Text(
                text = "Theme Rotation",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Motivation", "Discipline", "Success").forEach { th ->
                    val isChecked = activeThemes.contains(th)
                    ThemeFilterChip(
                        name = th,
                        isChecked = isChecked,
                        onClick = {
                            if (isChecked) activeThemes.remove(th) else activeThemes.add(th)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isChecked = activeThemes.contains("Mindfulness")
                ThemeFilterChip(
                    name = "Mindfulness",
                    isChecked = isChecked,
                    onClick = {
                        if (isChecked) activeThemes.remove("Mindfulness") else activeThemes.add("Mindfulness")
                    }
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .bounceClick(scaleDown = 0.94f, onClick = {
                            Toast.makeText(context, "Add new theme dialog", Toast.LENGTH_SHORT).show()
                        })
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Theme",
                            fontSize = 12.sp,
                            color = PurpleAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 8. Video Duration
            SettingDropdownRow(
                label = "Video Duration",
                value = videoDuration,
                options = listOf("15 seconds", "30 seconds", "60 seconds"),
                onSelect = { videoDuration = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 9. Add Narration Toggle
            SettingSwitchRow(
                title = "Add Narration",
                checked = narrationEnabled,
                onCheckedChange = { narrationEnabled = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 10. Daily Generation Limit
            SettingDropdownRow(
                label = "Daily Generation Limit",
                value = dailyLimit,
                options = listOf("1", "2", "3", "5"),
                onSelect = { dailyLimit = it }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 11. "Save Settings" Tactile Purple Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(16.dp, RoundedCornerShape(14.dp), spotColor = Color(0x667C3AED))
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(PurplePrimary, PurpleDark)
                        )
                    )
                    .bounceClick(scaleDown = 0.96f, onClick = {
                        Toast.makeText(context, "Automation settings saved!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Save Settings",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0x20000000))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1432), Color(0xFF120E25))
                )
            )
            .border(1.dp, specularBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextWhite,
                    checkedTrackColor = PurplePrimary,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = CardSurfaceElevated
                )
            )
        }
    }
}

@Composable
private fun SettingDropdownRow(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val specularBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.03f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0x20000000))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1432), Color(0xFF120E25))
                )
            )
            .border(1.dp, specularBorder, RoundedCornerShape(16.dp))
            .bounceClick(scaleDown = 0.98f, onClick = { expanded = !expanded })
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = TextSecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(CardSurfaceElevated)
                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(text = opt, color = TextWhite) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeFilterChip(
    name: String,
    isChecked: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .shadow(if (isChecked) 6.dp else 0.dp, RoundedCornerShape(14.dp), spotColor = PurplePrimary)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isChecked) PurpleSurface else CardSurface)
            .border(
                1.dp,
                if (isChecked) PurplePrimary else CardBorder,
                RoundedCornerShape(14.dp)
            )
            .bounceClick(scaleDown = 0.93f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isChecked) PurplePrimary else TextWhite
            )
            if (isChecked) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
