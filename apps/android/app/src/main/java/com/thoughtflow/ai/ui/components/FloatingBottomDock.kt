package com.thoughtflow.ai.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.thoughtflow.ai.ui.theme.*

@Composable
fun FloatingBottomDock(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "fab_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Popup Quick-Action Dark Pill (From Reference Image 1)
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { 60 }, animationSpec = tween(200)),
            exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { 60 }, animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 78.dp)
        ) {
            Row(
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x40000000))
                    .clip(RoundedCornerShape(28.dp))
                    .background(DarkPurplePill)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionIcon(
                    icon = Icons.Outlined.Lightbulb,
                    contentDesc = "Generate Thought",
                    onClick = {
                        expanded = false
                        onActionSelected("generate")
                    }
                )
                QuickActionIcon(
                    icon = Icons.Outlined.SmartDisplay,
                    contentDesc = "Video Studio",
                    onClick = {
                        expanded = false
                        onActionSelected("studio")
                    }
                )
                QuickActionIcon(
                    icon = Icons.Outlined.Upload,
                    contentDesc = "YouTube Publish",
                    onClick = {
                        expanded = false
                        onActionSelected("youtube")
                    }
                )
            }
        }

        // 2. Main White Floating Navigation Dock (From Reference Image 1 & 2)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color(0x1A0F172A))
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Tabs
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockTabItem(
                        icon = Icons.Outlined.Public,
                        selected = currentTab == "dashboard",
                        onClick = { onTabSelected("dashboard") }
                    )
                    DockTabItem(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        selected = currentTab == "library",
                        onClick = { onTabSelected("library") }
                    )
                }

                // Spacer for the center elevated hexagon
                Spacer(modifier = Modifier.width(64.dp))

                // Right Tabs
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockTabItem(
                        icon = Icons.Outlined.Notifications,
                        selected = currentTab == "activity",
                        hasBadge = true,
                        onClick = { onTabSelected("activity") }
                    )
                    DockTabItem(
                        icon = Icons.Outlined.PersonOutline,
                        selected = currentTab == "settings",
                        onClick = { onTabSelected("settings") }
                    )
                }
            }
        }

        // 3. Center Elevated Cyan Hexagon FAB Button (From Reference Image 1 & 2)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-14).dp)
                .size(58.dp)
                .shadow(12.dp, HexagonShape(), spotColor = CyanFab)
                .clip(HexagonShape())
                .background(CyanFab)
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Quick Actions",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotationAngle)
            )
        }
    }
}

@Composable
private fun DockTabItem(
    icon: ImageVector,
    selected: Boolean,
    hasBadge: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) CyanFab else Color(0xFF64748B),
            modifier = Modifier.size(24.dp)
        )
        if (hasBadge) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun QuickActionIcon(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x26FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}
