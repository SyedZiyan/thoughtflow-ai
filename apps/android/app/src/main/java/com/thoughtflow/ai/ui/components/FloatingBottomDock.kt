package com.thoughtflow.ai.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtflow.ai.ui.theme.*

/**
 * 5-Tab Clean Studio Luxury Navigation Dock:
 * [Home] | [Create] | [Library] | [Analytics] | [Settings]
 * - Understated neutral obsidian & titanium dark styling
 * - Smooth Apple fluid sliding capsule (zero harsh neon or rainbow shifting)
 * - Pure high-contrast white active typography and clean slate inactive states
 */
@Composable
fun FloatingBottomDock(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetTabIndex = when (currentTab) {
        "dashboard", "home" -> 0
        "generator", "studio", "create" -> 1
        "library" -> 2
        "analytics" -> 3
        "settings" -> 4
        else -> 0
    }

    // Buttery smooth Apple fluid easing (critically damped - zero jitter)
    val animatedTabIndex by animateFloatAsState(
        targetValue = targetTabIndex.toFloat(),
        animationSpec = tween(
            durationMillis = 260,
            easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
        ),
        label = "dock_sliding_capsule"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color(0x60000000))
                .clip(RoundedCornerShape(22.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            val totalWidth = maxWidth
            val tabWidth = totalWidth / 5

            // 1. Clean Subtle Graphite Sliding Capsule
            Box(
                modifier = Modifier
                    .offset(x = tabWidth * animatedTabIndex + 3.dp)
                    .width(tabWidth - 6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF22252E))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(16.dp)
                    )
            )

            // 2. Row of 5 Tab Buttons
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockTabButton(
                    label = "Home",
                    selected = targetTabIndex == 0,
                    iconSelected = Icons.Filled.Home,
                    iconUnselected = Icons.Outlined.Home,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabSelected("dashboard") }
                )

                DockTabButton(
                    label = "Create",
                    selected = targetTabIndex == 1,
                    iconSelected = Icons.Filled.AutoAwesome,
                    iconUnselected = Icons.Outlined.AutoAwesome,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabSelected("create") }
                )

                DockTabButton(
                    label = "Library",
                    selected = targetTabIndex == 2,
                    iconSelected = Icons.Filled.VideoLibrary,
                    iconUnselected = Icons.Outlined.VideoLibrary,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabSelected("library") }
                )

                DockTabButton(
                    label = "Analytics",
                    selected = targetTabIndex == 3,
                    iconSelected = Icons.Filled.BarChart,
                    iconUnselected = Icons.Outlined.BarChart,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabSelected("analytics") }
                )

                DockTabButton(
                    label = "Settings",
                    selected = targetTabIndex == 4,
                    iconSelected = Icons.Filled.Settings,
                    iconUnselected = Icons.Outlined.Settings,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabSelected("settings") }
                )
            }
        }
    }
}

@Composable
private fun DockTabButton(
    label: String,
    selected: Boolean,
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val activeColor = TextWhite
    val inactiveColor = TextMuted

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1.0f,
        animationSpec = tween(
            durationMillis = 220,
            easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
        ),
        label = "tab_icon_scale"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .bounceClick(scaleDown = 0.95f, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) iconSelected else iconUnselected,
            contentDescription = label,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier
                .size(20.dp)
                .scale(iconScale)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) activeColor else inactiveColor
        )
    }
}
