package com.thoughtflow.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thoughtflow.ai.ui.theme.*
import com.thoughtflow.ai.util.PhotoExportUtil

/**
 * Universal High-Fidelity Compose Canvas for Dynamic Photographic Styles.
 * Renders any generated DynamicStyle live on screen (Torn Paper, Vintage Story,
 * Polaroid Frame, Minimalist Swiss Editorial, or Glassmorphic Frost).
 */
@Composable
fun DynamicPhotoCanvas(
    style: PhotoExportUtil.DynamicStyle,
    quote: String,
    author: String,
    modifier: Modifier = Modifier
) {
    val highlightComposeColor = Color(style.highlightColor)
    val fontFam = when (style.fontFamily) {
        "sans" -> FontFamily.Default
        "script" -> FontFamily.Cursive
        else -> FontFamily.Serif
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
    ) {
        // 1. Photographic Scenery Background
        AsyncImage(
            model = "file:///android_asset/backgrounds/${style.assetFilename}",
            contentDescription = style.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Procedural Card Template Layout
        when (style.cardStyle) {
            "torn_paper" -> {
                // Style 1: Torn Paper Sticker Note (Reference Image 2)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(14.dp, RoundedCornerShape(16.dp), spotColor = Color(0x75000000))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFDFBF7))
                            .border(1.5.dp, Color(0xFFE7E5E4), RoundedCornerShape(16.dp))
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = style.ornament,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1C1917),
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Thought of the Day",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF292524)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Highlighted Quote Text
                            val words = quote.split(" ")
                            val highlightIndex = (words.size / 2).coerceIn(0, words.size - 1)
                            val annotatedQuote = buildAnnotatedString {
                                words.forEachIndexed { idx, word ->
                                    if (idx == highlightIndex) {
                                        withStyle(
                                            SpanStyle(
                                                background = highlightComposeColor,
                                                color = Color(0xFF1C1917),
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append(" $word ")
                                        }
                                    } else {
                                        append("$word ")
                                    }
                                }
                            }

                            Text(
                                text = annotatedQuote,
                                fontSize = if (quote.length > 90) 14.sp else 16.5.sp,
                                fontFamily = fontFam,
                                color = Color(0xFF1C1917),
                                textAlign = TextAlign.Center,
                                lineHeight = 23.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("✈ - - -", fontSize = 12.sp, color = Color(0xFF78716C))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "— $author",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF44403C)
                                )
                            }
                        }
                    }
                }
            }

            "vintage_story" -> {
                // Style 2: Vintage Story Metaphor (Reference Image 3)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x99000000),
                                    Color(0x55000000),
                                    Color(0xDD000000)
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✦  DAILY MEDITATION  ✦",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 2.sp
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = style.ornament,
                                fontSize = 18.sp,
                                color = highlightComposeColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = quote,
                                fontSize = if (quote.length > 90) 16.sp else 19.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = fontFam,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 26.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = style.ornament,
                                fontSize = 18.sp,
                                color = highlightComposeColor
                            )
                        }

                        Text(
                            text = "— $author",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                    }
                }
            }

            "polaroid" -> {
                // Style 3: Retro Polaroid Film Frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = Color(0x80000000))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(14.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Dark cutout preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x25000000))
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "\"$quote\"",
                                fontSize = if (quote.length > 90) 13.5.sp else 15.sp,
                                fontFamily = FontFamily.Serif,
                                color = Color(0xFF1E293B),
                                textAlign = TextAlign.Center,
                                lineHeight = 21.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "— $author",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = highlightComposeColor,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            "minimal_editorial" -> {
                // Style 4: Minimalist Swiss Editorial Card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(14.dp, RoundedCornerShape(16.dp), spotColor = Color(0x60000000))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xEE13151A))
                            .border(1.dp, Color(0xFF2A2D36), RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(highlightComposeColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DAILY INSIGHT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 1.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = quote,
                                fontSize = if (quote.length > 90) 15.sp else 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFam,
                                color = Color.White,
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "— $author",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = highlightComposeColor
                            )
                        }
                    }
                }
            }

            else -> {
                // Style 5: Glassmorphic Frost Card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x50000000))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x40000000))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                            .padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "“",
                                fontSize = 38.sp,
                                fontFamily = FontFamily.Serif,
                                color = highlightComposeColor
                            )
                            Text(
                                text = quote,
                                fontSize = if (quote.length > 90) 15.sp else 18.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = fontFam,
                                color = TextWhite,
                                textAlign = TextAlign.Center,
                                lineHeight = 25.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "— $author",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // Top Subtle Badge: Dynamic Style Title
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xBB0A0A0C))
                .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = style.title.take(24),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
        }
    }
}
