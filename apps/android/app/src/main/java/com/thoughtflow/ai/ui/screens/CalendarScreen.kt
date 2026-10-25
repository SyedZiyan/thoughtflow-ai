package com.thoughtflow.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.thoughtflow.ai.ui.components.CinematicBackdrop
import com.thoughtflow.ai.ui.components.bounceClick
import com.thoughtflow.ai.ui.theme.*

data class ScheduleFeedItem(
    val dayLabel: String,
    val title: String,
    val statusText: String,
    val statusColor: Color,
    val isYouTube: Boolean = false
)

/**
 * Screen 7: Publishing Calendar (Elite Polish Edition)
 * - Month selector: < December 2024 > with tactile controls
 * - 7-Day calendar grid with scheduled indicators
 * - Specular Day schedule cards
 * - "Add / Reschedule" tactile purple button
 */
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedDay by remember { mutableStateOf(18) }

    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val totalDays = (1..31).toList()
    val scheduledDays = setOf(15, 18, 19, 20, 22, 25)

    val scheduleFeed = listOf(
        ScheduleFeedItem(
            dayLabel = "Today",
            title = "Small steps create extraordinary change.",
            statusText = "✓ Published • 8:00 AM",
            statusColor = StatusGreen,
            isYouTube = true
        ),
        ScheduleFeedItem(
            dayLabel = "Tomorrow",
            title = "A calm mind is a powerful mind.",
            statusText = "⏰ Scheduled • 8:00 AM",
            statusColor = StatusAmber,
            isYouTube = false
        ),
        ScheduleFeedItem(
            dayLabel = "Dec 20",
            title = "Discipline today creates freedom tomorrow.",
            statusText = "⏰ Scheduled • 8:00 AM",
            statusColor = StatusAmber,
            isYouTube = false
        )
    )

    val specularBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            Color.White.copy(alpha = 0.03f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
                    text = "Publishing Calendar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Month Selector: < December 2024 >
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /* Prev Month */ },
                    modifier = Modifier.bounceClick(scaleDown = 0.90f, onClick = {})
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = TextSecondary
                    )
                }

                Text(
                    text = "December 2024",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                IconButton(
                    onClick = { /* Next Month */ },
                    modifier = Modifier.bounceClick(scaleDown = 0.90f, onClick = {})
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 7-Day Grid Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Days Grid (1 - 31) with Spring Bounce
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(totalDays) { day ->
                        val isSelected = day == selectedDay
                        val hasDot = scheduledDays.contains(day)

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .bounceClick(scaleDown = 0.88f, onClick = { selectedDay = day })
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .shadow(if (isSelected) 8.dp else 0.dp, CircleShape, spotColor = PurplePrimary)
                                    .clip(CircleShape)
                                    .background(if (isSelected) PurplePrimary else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$day",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextWhite else TextSecondary
                                )
                            }

                            if (hasDot) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) PurplePrimary else PurpleAccent)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Schedule Feed for Selected Day
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scheduleFeed) { item ->
                    CalendarScheduleCard(
                        item = item,
                        onClick = {
                            Toast.makeText(context, "${item.dayLabel}: '${item.title}' (${item.statusText})", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. "Add / Reschedule" Tactile Purple Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(16.dp, RoundedCornerShape(14.dp), spotColor = Color(0x667C3AED))
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(PurplePrimary, PurpleDark)
                        )
                    )
                    .bounceClick(scaleDown = 0.96f, onClick = {
                        Toast.makeText(context, "Schedule updated for Dec $selectedDay, 2024", Toast.LENGTH_SHORT).show()
                    }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add / Reschedule",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun CalendarScheduleCard(
    item: ScheduleFeedItem,
    onClick: () -> Unit
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
                    colors = listOf(Color(0xFF191330), Color(0xFF110D24))
                )
            )
            .border(1.dp, specularBorder, RoundedCornerShape(16.dp))
            .bounceClick(scaleDown = 0.97f, onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.dayLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.width(62.dp)
            )

            // Small Living Thumbnail
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardSurfaceElevated)
            ) {
                CinematicBackdrop(enableKenBurns = false, dimAlpha = 0.2f, modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(item.statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.statusText,
                        fontSize = 11.sp,
                        color = item.statusColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
