package com.thoughtflow.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtflow.ai.ui.theme.*

/**
 * Creation Format Selector Modal:
 * Asks the user whether they want to generate a "Photo" (Quote Image) or "Video" (Short/Reel).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationFormatDialog(
    onDismissRequest: () -> Unit,
    onSelectVideo: () -> Unit,
    onSelectPhoto: () -> Unit
) {
    val specularBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.22f),
            Color(0x358B5CF6),
            Color.White.copy(alpha = 0.05f)
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF130F24),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Create New Content",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose what format you want to generate:",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Option 1: Video Short / Reel
            FormatOptionCard(
                icon = Icons.Default.Videocam,
                iconColor = PurpleAccent,
                title = "Video (Shorts & Reels)",
                badge = "1080p MP4",
                badgeColor = PurplePrimary,
                description = "Animated vertical video with moving subtitles, cinematic background & music for YouTube Shorts and Reels.",
                onClick = {
                    onDismissRequest()
                    onSelectVideo()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Option 2: Photo Quote Card / Poster
            FormatOptionCard(
                icon = Icons.Default.Image,
                iconColor = Color(0xFF38BDF8),
                title = "Photo (Quote Card)",
                badge = "HD Image",
                badgeColor = Color(0xFF38BDF8),
                description = "High-resolution visual quote poster with gradient typography for Instagram Posts, WhatsApp Status & Community.",
                onClick = {
                    onDismissRequest()
                    onSelectPhoto()
                }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FormatOptionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    badge: String,
    badgeColor: Color,
    description: String,
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
            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Color(0x30000000))
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1C1736), Color(0xFF120E26))
                )
            )
            .border(1.dp, specularBorder, RoundedCornerShape(18.dp))
            .bounceClick(scaleDown = 0.96f, onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = 0.15f))
                    .border(1.dp, iconColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.18f))
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
