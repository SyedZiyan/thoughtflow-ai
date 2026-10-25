package com.thoughtflow.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtflow.ai.ui.components.bounceClick
import com.thoughtflow.ai.ui.theme.*

data class ExecutionLogItem(
    val title: String,
    val timestamp: String,
    val type: String, // "success", "failed", "info"
    val icon: ImageVector,
    val iconColor: Color,
    val detailText: String? = null
)

/**
 * Screen 10: Execution Logs (Elite Polish Edition)
 * - All | Success | Failed | Info filter pills with spring bounce
 * - Specular Glass activity feed with colored event status icons
 */
@Composable
fun LogsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filterOptions = listOf("All", "Success", "Failed", "Info")
    var selectedFilter by remember { mutableStateOf("All") }

    val specularBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            Color.White.copy(alpha = 0.03f)
        )
    )

    val allLogs = listOf(
        ExecutionLogItem(
            title = "Video published successfully",
            timestamp = "Dec 18, 2024 8:00 AM",
            type = "success",
            icon = Icons.Default.CheckCircle,
            iconColor = StatusGreen
        ),
        ExecutionLogItem(
            title = "Uploading to YouTube",
            timestamp = "Dec 18, 2024 7:58 AM",
            type = "info",
            icon = Icons.Outlined.CloudUpload,
            iconColor = PurplePrimary
        ),
        ExecutionLogItem(
            title = "Video rendered",
            timestamp = "Dec 18, 2024 7:45 AM",
            type = "success",
            icon = Icons.Outlined.SmartDisplay,
            iconColor = StatusBlue
        ),
        ExecutionLogItem(
            title = "Generating video",
            timestamp = "Dec 18, 2024 7:40 AM",
            type = "info",
            icon = Icons.Outlined.AutoAwesome,
            iconColor = PurpleAccent
        ),
        ExecutionLogItem(
            title = "AI thought generated",
            timestamp = "Dec 18, 2024 7:39 AM",
            type = "info",
            icon = Icons.Outlined.Lightbulb,
            iconColor = StatusBlue
        ),
        ExecutionLogItem(
            title = "Daily job started",
            timestamp = "Dec 18, 2024 7:39 AM",
            type = "info",
            icon = Icons.Outlined.Schedule,
            iconColor = StatusBlue
        ),
        ExecutionLogItem(
            title = "Failed to render video",
            timestamp = "Dec 17, 2024 7:45 AM",
            type = "failed",
            icon = Icons.Default.Cancel,
            iconColor = StatusRed,
            detailText = "FFmpeg process failed. Retrying..."
        )
    )

    val filteredLogs = remember(selectedFilter) {
        when (selectedFilter) {
            "Success" -> allLogs.filter { it.type == "success" }
            "Failed" -> allLogs.filter { it.type == "failed" }
            "Info" -> allLogs.filter { it.type == "info" }
            else -> allLogs
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
                    text = "Execution Logs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            // 2. Filter Pills Row with Spring Bounce
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filterOptions) { opt ->
                    val isSelected = selectedFilter == opt
                    val pillBorder = if (isSelected) Brush.linearGradient(listOf(PurplePrimary, PurpleAccent)) else specularBorder
                    Box(
                        modifier = Modifier
                            .shadow(if (isSelected) 8.dp else 0.dp, RoundedCornerShape(18.dp), spotColor = PurplePrimary)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) PurplePrimary else CardSurface)
                            .border(1.dp, pillBorder, RoundedCornerShape(18.dp))
                            .bounceClick(scaleDown = 0.92f, onClick = { selectedFilter = opt })
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = opt,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) TextWhite else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Execution Logs List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredLogs) { log ->
                    LogCard(log = log)
                }
            }
        }
    }
}

@Composable
private fun LogCard(log: ExecutionLogItem) {
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
                    colors = listOf(Color(0xFF191330), Color(0xFF110D24))
                )
            )
            .border(1.dp, specularBorder, RoundedCornerShape(16.dp))
            .bounceClick(scaleDown = 0.97f, onClick = { })
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(log.iconColor.copy(alpha = 0.15f))
                    .border(0.8.dp, log.iconColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = log.icon,
                    contentDescription = null,
                    tint = log.iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.timestamp,
                    fontSize = 12.sp,
                    color = TextMuted
                )

                if (log.detailText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = log.detailText,
                        fontSize = 12.sp,
                        color = StatusRed.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
