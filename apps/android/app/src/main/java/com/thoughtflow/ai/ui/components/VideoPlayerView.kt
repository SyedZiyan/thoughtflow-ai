package com.thoughtflow.ai.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.viewinterop.AndroidView
import com.thoughtflow.ai.data.api.ApiClient
import com.thoughtflow.ai.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * High-performance Video Player Component for ThoughtFlow AI.
 * Plays 9:16 Shorts, local MP4 files, and streaming backend video storage
 * with physical spring controls, scrubber timeline, audio mute toggle, and auto-looping.
 */
@Composable
fun VideoPlayerView(
    videoUrl: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    autoPlay: Boolean = true,
    showControls: Boolean = true,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var isMuted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    // Resolve URL to backend if relative
    val resolvedUrl = remember(videoUrl) {
        resolveMediaUrl(videoUrl)
    }

    // Controls auto-hide timer
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    // Playback position polling loop
    LaunchedEffect(videoViewRef, isPlaying) {
        while (isActive) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    currentPositionMs = vv.currentPosition.toLong()
                    if (vv.duration > 0) {
                        durationMs = vv.duration.toLong()
                    }
                }
            }
            delay(250)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Underlying Android VideoView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setZOrderOnTop(false)

                    setOnPreparedListener { mp ->
                        mediaPlayerRef = mp
                        mp.isLooping = true
                        isLoading = false
                        hasError = false
                        durationMs = mp.duration.toLong()
                        if (isMuted) mp.setVolume(0f, 0f) else mp.setVolume(1f, 1f)
                        if (autoPlay) {
                            start()
                            isPlaying = true
                        }
                    }

                    setOnErrorListener { _, what, extra ->
                        isLoading = false
                        hasError = true
                        errorMessage = "Playback issue ($what:$extra)"
                        true
                    }

                    setOnCompletionListener {
                        isPlaying = false
                    }

                    setVideoURI(Uri.parse(resolvedUrl))
                    videoViewRef = this
                }
            },
            update = { vv ->
                videoViewRef = vv
            }
        )

        // 2. Loading Spinner
        if (isLoading) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = PurpleAccent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // 3. Error Overlay with Retry
        if (hasError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD90D0B18))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = StatusAmber,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Video Stream Connecting...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage ?: "Connecting to video rendering server",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PurplePrimary)
                        .clickable {
                            hasError = false
                            isLoading = true
                            videoViewRef?.setVideoURI(Uri.parse(resolvedUrl))
                            videoViewRef?.start()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Retry Playback", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
            }
        }

        // 4. Center Play/Pause Indicator (when paused)
        if (!isLoading && !hasError && !isPlaying) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(16.dp, CircleShape, spotColor = Color(0x807C3AED))
                    .clip(CircleShape)
                    .background(Color(0xB315102A))
                    .border(1.5.dp, PurpleAccent, CircleShape)
                    .bounceClick(scaleDown = 0.90f) {
                        videoViewRef?.start()
                        isPlaying = true
                        controlsVisible = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = TextWhite,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // 5. Sleek Overlay Controls
        if (showControls) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x99000000),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color(0xCC000000)
                                )
                            )
                        )
                ) {
                    // Top Bar: Title + Close Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!title.isNullOrEmpty()) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        // Mute / Unmute button
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                mediaPlayerRef?.let { mp ->
                                    if (isMuted) mp.setVolume(0f, 0f) else mp.setVolume(1f, 1f)
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x55000000))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Audio",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (onClose != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x55000000))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Bottom Bar: Play/Pause, Scrubber, Duration Counter
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        // Scrubber Slider
                        val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        Slider(
                            value = progress,
                            onValueChange = { newProgress ->
                                val target = (newProgress * durationMs).toInt()
                                videoViewRef?.seekTo(target)
                                currentPositionMs = target.toLong()
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = PurpleAccent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play/Pause button
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        videoViewRef?.let { vv ->
                                            if (vv.isPlaying) {
                                                vv.pause()
                                                isPlaying = false
                                            } else {
                                                vv.start()
                                                isPlaying = true
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Toggle Play",
                                        tint = TextWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                val curStr = formatTime(currentPositionMs)
                                val durStr = formatTime(durationMs)
                                Text(
                                    text = "$curStr / $durStr",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }

                            // Replay Icon
                            IconButton(
                                onClick = {
                                    videoViewRef?.seekTo(0)
                                    videoViewRef?.start()
                                    isPlaying = true
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Replay",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Resolves video URLs:
 * - If starts with http:// or https://, returns as-is.
 * - If starts with /storage/ or relative, prepends backend host.
 * - If empty, falls back to default sample MP4 from backend.
 */
fun resolveMediaUrl(rawUrl: String?): String {
    if (rawUrl.isNullOrBlank()) {
        return "${ApiClient.getHost()}/storage/videos/thought_phase3_test_01.mp4"
    }
    return when {
        rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
        rawUrl.startsWith("/") -> "${ApiClient.getHost()}$rawUrl"
        else -> "${ApiClient.getHost()}/storage/videos/$rawUrl"
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
