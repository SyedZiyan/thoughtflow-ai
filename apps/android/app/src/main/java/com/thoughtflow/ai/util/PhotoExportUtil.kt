package com.thoughtflow.ai.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PhotoExportUtil {

    data class ThemeConfig(
        val code: String,
        val displayName: String,
        val assetFilename: String,
        val cardStyle: String // "torn_paper", "vintage_story", "sunset_balcony", "glassmorphic"
    )

    data class DynamicStyle(
        val id: String,
        val title: String,
        val assetFilename: String,
        val cardStyle: String, // "torn_paper", "vintage_story", "polaroid", "minimal_editorial", "glassmorphic"
        val highlightColor: Long, // e.g. 0xFFFDE047L
        val ornament: String, // "\ | /", "— ❧ —", "— ✦ —", "❖ ❖ ❖", "✈ - - -", "★ ★ ★"
        val fontFamily: String // "serif", "sans", "script"
    )

    val SCENIC_PHOTOS = listOf(
        Pair("user_mountain_hiker.jpg", "Alpine Trail"),
        Pair("mountain_ridge_sunrise.jpg", "Sunrise Summit"),
        Pair("mountain_summit.jpg", "Dramatic Peak"),
        Pair("mountain_sunset_hiker.jpg", "Sunset Ridge"),
        Pair("user_balcony_sunset.jpg", "Balcony Sunset"),
        Pair("balcony_sunset_city.jpg", "City Skyline Dusk"),
        Pair("misty_forest.jpg", "Misty Redwood"),
        Pair("starry_night.jpg", "Starry Cosmos"),
        Pair("starry_night_cabin.jpg", "Northern Cabin"),
        Pair("ocean_sunset.jpg", "Ocean Twilight"),
        Pair("golden_desert.jpg", "Golden Dunes"),
        Pair("rainy_window.jpg", "Rainy Evening Window"),
        Pair("fairy_lights_dusk.jpg", "Fairy Lights Terrace"),
        Pair("warm_lantern_window.jpg", "Cozy Lantern Window"),
        Pair("warm_study_desk.jpg", "Classical Library Desk"),
        Pair("night_city_lights.jpg", "Metropolis Night Glow")
    )

    val CARD_STYLES = listOf(
        Pair("torn_paper", "Torn Sticker Note"),
        Pair("vintage_story", "Vintage Storybook"),
        Pair("polaroid", "Retro Polaroid"),
        Pair("minimal_editorial", "Swiss Editorial"),
        Pair("glassmorphic", "Frosted Glass")
    )

    val ACCENT_COLORS = listOf(
        Pair(0xFFFDE047L, "Sunburst Yellow"),
        Pair(0xFF86EFACL, "Matcha Mint"),
        Pair(0xFFFDBA74L, "Warm Peach"),
        Pair(0xFF7DD3FCL, "Ice Cyan"),
        Pair(0xFFFDA4AFL, "Petal Rose"),
        Pair(0xFFF59E0BL, "Amber Gold"),
        Pair(0xFFC084FCL, "Amethyst Purple")
    )

    val ORNAMENTS = listOf(
        "\\   |   /",
        "— ❧ —",
        "— ✦ —",
        "❖ ❖ ❖",
        "✈ - - -",
        "★ ★ ★",
        "✧ ✧ ✧"
    )

    val THEMES = listOf(
        ThemeConfig("mountain_path", "🏔️ Alpine Path (Sticker)", "user_mountain_hiker.jpg", "torn_paper"),
        ThemeConfig("cobblestone", "🏛️ Vintage Story (Serif)", "mountain_ridge_sunrise.jpg", "vintage_story"),
        ThemeConfig("sunset_balcony", "🌅 Sunset Balcony", "user_balcony_sunset.jpg", "sunset_balcony"),
        ThemeConfig("mountain_summit", "⛰️ Mountain Summit", "mountain_summit.jpg", "vintage_story"),
        ThemeConfig("misty_redwood", "🌲 Emerald Redwood", "misty_forest.jpg", "glassmorphic"),
        ThemeConfig("starry_cosmos", "✨ Starry Cosmos", "starry_night.jpg", "glassmorphic")
    )

    /**
     * Generates a brand-new, unique visual theme & layout combination randomly every time!
     */
    fun generateRandomStyle(): DynamicStyle {
        val photo = SCENIC_PHOTOS.random()
        val card = CARD_STYLES.random()
        val accent = ACCENT_COLORS.random()
        val ornament = ORNAMENTS.random()
        val font = listOf("serif", "sans", "script").random()

        val title = "${photo.second} • ${card.second} (${accent.second})"
        val id = "dyn_${System.currentTimeMillis() % 1000000}"

        return DynamicStyle(
            id = id,
            title = title,
            assetFilename = photo.first,
            cardStyle = card.first,
            highlightColor = accent.first,
            ornament = ornament,
            fontFamily = font
        )
    }

    /**
     * Renders a high-resolution 1080p visual Quote Photo Card using genuine photographic scenery
     * and editorial typography matching the selected or procedural dynamic style.
     */
    fun createQuoteBitmap(
        context: Context,
        quote: String,
        author: String = "ThoughtFlow AI",
        aspectRatio: String = "9:16", // "9:16", "1:1", "4:5"
        styleTheme: String = "mountain_path",
        dynamicStyle: DynamicStyle? = null
    ): Bitmap {
        val (width, height) = when (aspectRatio) {
            "1:1" -> Pair(1080, 1080)
            "4:5" -> Pair(1080, 1350)
            else -> Pair(1080, 1920) // 9:16 vertical story
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val assetFilename = dynamicStyle?.assetFilename
            ?: THEMES.find { it.code == styleTheme }?.assetFilename
            ?: "user_mountain_hiker.jpg"

        val cardStyle = dynamicStyle?.cardStyle
            ?: THEMES.find { it.code == styleTheme }?.cardStyle
            ?: "torn_paper"

        val highlightColor = dynamicStyle?.highlightColor ?: 0xFFFDE047L
        val ornament = dynamicStyle?.ornament ?: "\\   |   /"
        val fontFam = dynamicStyle?.fontFamily ?: "serif"

        // 1. Draw Real High-Res Photographic Scenery
        val bgBitmap = loadAssetBitmap(context, "backgrounds/$assetFilename", width, height)
        if (bgBitmap != null) {
            canvas.drawBitmap(bgBitmap, 0f, 0f, null)
        } else {
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(0xFF13151A.toInt(), 0xFF090A0D.toInt(), 0xFF000000.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        }

        // 2. Render Template Overlay according to Style
        when (cardStyle) {
            "torn_paper" -> renderTornPaperCard(canvas, width, height, quote, author, aspectRatio, highlightColor, ornament, fontFam)
            "vintage_story" -> renderVintageStoryOverlay(canvas, width, height, quote, author, aspectRatio, highlightColor, ornament, fontFam)
            "polaroid" -> renderPolaroidCard(canvas, width, height, quote, author, aspectRatio, highlightColor, fontFam)
            "minimal_editorial" -> renderMinimalEditorialCard(canvas, width, height, quote, author, aspectRatio, highlightColor, fontFam)
            else -> renderGlassmorphicCard(canvas, width, height, quote, author, aspectRatio, highlightColor)
        }

        return bitmap
    }

    /**
     * Style 1: Torn Paper Sticker Note (Reference Image 2)
     */
    private fun renderTornPaperCard(
        canvas: Canvas,
        width: Int,
        height: Int,
        quote: String,
        author: String,
        aspectRatio: String,
        highlightColor: Long = 0xFFFDE047L,
        ornament: String = "\\   |   /",
        fontFam: String = "serif"
    ) {
        val cardWidth = width * 0.88f
        val cardHeight = if (aspectRatio == "1:1") height * 0.65f else height * 0.44f
        val cardLeft = (width - cardWidth) / 2f
        val cardTop = if (aspectRatio == "1:1") height * 0.18f else height * 0.20f
        val cardRect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)

        // Drop shadow for realistic floating paper effect
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            alpha = 75
            maskFilter = BlurMaskFilter(28f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(
            RectF(cardRect.left + 4f, cardRect.top + 16f, cardRect.right + 4f, cardRect.bottom + 16f),
            24f, 24f, shadowPaint
        )

        // Off-white organic paper card
        val paperPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFFFDFBF7.toInt()
        }
        canvas.drawRoundRect(cardRect, 22f, 22f, paperPaint)

        // Subtle textured border
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = 0xFFE7E5E4.toInt()
        }
        canvas.drawRoundRect(cardRect, 22f, 22f, borderPaint)

        // Doodled Sunburst / Ornament
        val sunPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF1C1917.toInt()
            textSize = 36f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(ornament, width / 2f, cardTop - 18f, sunPaint)

        // "Thought of the Day" Header
        val headerPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF292524.toInt()
            textSize = 34f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Thought of the Day", width / 2f, cardTop + 65f, headerPaint)

        // Main Quote text
        val textTypeface = when (fontFam) {
            "sans" -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            "script" -> Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            else -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = 0xFF1C1917.toInt()
            textSize = when {
                quote.length > 130 -> 46f
                quote.length > 80 -> 52f
                else -> 58f
            }
            typeface = textTypeface
            textAlign = Paint.Align.CENTER
        }

        // Draw highlighter mark behind keyword
        val hlPaint = Paint().apply {
            isAntiAlias = true
            color = highlightColor.toInt()
            alpha = 230
        }

        val textWidth = (cardWidth - 90f).toInt()
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(quote, 0, quote.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(14f, 1.22f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(quote, textPaint, textWidth, Layout.Alignment.ALIGN_CENTER, 1.22f, 14f, false)
        }

        val textY = cardTop + 140f
        val hlRect = RectF(width / 2f - 110f, textY - 4f, width / 2f + 110f, textY + 54f)
        canvas.save()
        canvas.rotate(-1.5f, hlRect.centerX(), hlRect.centerY())
        canvas.drawRoundRect(hlRect, 8f, 8f, hlPaint)
        canvas.restore()

        canvas.save()
        canvas.translate(cardLeft + 45f, textY)
        staticLayout.draw(canvas)
        canvas.restore()

        // Paper Plane Doodle ✈ with dotted trajectory
        val planePaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF78716C.toInt()
            textSize = 32f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("✈ - - -", cardRect.right - 28f, cardRect.bottom - 24f, planePaint)

        // Creator signature below card
        val authorPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 3f, Color.BLACK)
        }
        val sigY = if (aspectRatio == "1:1") height - 50f else height - 120f
        canvas.drawText("✦  $author  ✦", width / 2f, sigY, authorPaint)
    }

    /**
     * Style 2: Vintage Story Metaphor (Reference Image 3)
     */
    private fun renderVintageStoryOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        quote: String,
        author: String,
        aspectRatio: String,
        highlightColor: Long = 0xFFF59E0BL,
        ornament: String = "— ❧ —",
        fontFam: String = "serif"
    ) {
        val scrimPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(0xBB000000.toInt(), 0x55000000, 0xDD000000.toInt()),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        val centerY = if (aspectRatio == "1:1") height * 0.45f else height * 0.38f

        // Top Ornament
        val ornPaint = Paint().apply {
            isAntiAlias = true
            color = highlightColor.toInt()
            textSize = 36f
            textAlign = Paint.Align.CENTER
            setShadowLayer(10f, 0f, 4f, Color.BLACK)
        }
        canvas.drawText(ornament, width / 2f, centerY - 140f, ornPaint)

        val textTypeface = when (fontFam) {
            "sans" -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            "script" -> Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            else -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = when {
                quote.length > 120 -> 48f
                quote.length > 70 -> 56f
                else -> 64f
            }
            typeface = textTypeface
            textAlign = Paint.Align.CENTER
            setShadowLayer(16f, 0f, 6f, Color.BLACK)
        }

        val padding = 110
        val textWidth = width - (padding * 2)
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(quote, 0, quote.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(18f, 1.28f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(quote, textPaint, textWidth, Layout.Alignment.ALIGN_CENTER, 1.28f, 18f, false)
        }

        val startY = centerY - (staticLayout.height / 2f)
        canvas.save()
        canvas.translate(padding.toFloat(), startY)
        staticLayout.draw(canvas)
        canvas.restore()

        // Bottom Ornament
        canvas.drawText(ornament, width / 2f, startY + staticLayout.height + 65f, ornPaint)

        // Author tag
        val authorPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            alpha = 230
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 4f, Color.BLACK)
        }
        canvas.drawText("— $author", width / 2f, startY + staticLayout.height + 140f, authorPaint)
    }

    /**
     * Style 3: Retro Polaroid Film Frame
     */
    private fun renderPolaroidCard(
        canvas: Canvas,
        width: Int,
        height: Int,
        quote: String,
        author: String,
        aspectRatio: String,
        highlightColor: Long = 0xFFFDBA74L,
        fontFam: String = "script"
    ) {
        val cardWidth = width * 0.86f
        val cardHeight = if (aspectRatio == "1:1") height * 0.75f else height * 0.52f
        val cardLeft = (width - cardWidth) / 2f
        val cardTop = (height - cardHeight) / 2.2f
        val cardRect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)

        // Polaroid Frame Drop Shadow
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            alpha = 85
            maskFilter = BlurMaskFilter(32f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(cardRect, 18f, 18f, shadowPaint)

        // Crisp White Polaroid Border
        val framePaint = Paint().apply {
            isAntiAlias = true
            color = 0xFFF8FAFC.toInt()
        }
        canvas.drawRoundRect(cardRect, 16f, 16f, framePaint)

        // Dark Photo Cutout
        val margin = 32f
        val photoHeight = cardHeight * 0.68f
        val cutoutRect = RectF(cardLeft + margin, cardTop + margin, cardRect.right - margin, cardTop + photoHeight)
        val cutoutPaint = Paint().apply {
            isAntiAlias = true
            color = 0x22000000
        }
        canvas.drawRoundRect(cutoutRect, 8f, 8f, cutoutPaint)

        // Polaroid bottom handwriting quote
        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = 0xFF1E293B.toInt()
            textSize = when {
                quote.length > 100 -> 38f
                else -> 44f
            }
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }

        val textWidth = (cardWidth - margin * 3).toInt()
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(quote, 0, quote.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(10f, 1.2f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(quote, textPaint, textWidth, Layout.Alignment.ALIGN_CENTER, 1.2f, 10f, false)
        }

        val textY = cutoutRect.bottom + 36f
        canvas.save()
        canvas.translate(cardLeft + margin * 1.5f, textY)
        staticLayout.draw(canvas)
        canvas.restore()

        // Signature
        val sigPaint = Paint().apply {
            isAntiAlias = true
            color = highlightColor.toInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("— $author", cardRect.right - margin, cardRect.bottom - 22f, sigPaint)
    }

    /**
     * Style 4: Minimalist Swiss Editorial Card
     */
    private fun renderMinimalEditorialCard(
        canvas: Canvas,
        width: Int,
        height: Int,
        quote: String,
        author: String,
        aspectRatio: String,
        highlightColor: Long = 0xFF86EFACL,
        fontFam: String = "sans"
    ) {
        val cardWidth = width * 0.88f
        val cardHeight = if (aspectRatio == "1:1") height * 0.65f else height * 0.46f
        val cardLeft = (width - cardWidth) / 2f
        val cardTop = (height - cardHeight) / 2.2f
        val cardRect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)

        // Dark Studio Card
        val cardPaint = Paint().apply {
            isAntiAlias = true
            color = 0xEE13151A.toInt()
        }
        canvas.drawRoundRect(cardRect, 18f, 18f, cardPaint)

        // Clean hairline border
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = 0xFF2A2D36.toInt()
        }
        canvas.drawRoundRect(cardRect, 18f, 18f, borderPaint)

        // Accent indicator dot
        val dotPaint = Paint().apply {
            isAntiAlias = true
            color = highlightColor.toInt()
        }
        canvas.drawCircle(cardLeft + 45f, cardTop + 55f, 12f, dotPaint)

        // Category Tag
        val tagPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            alpha = 180
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.2f
        }
        canvas.drawText("DAILY INSIGHT", cardLeft + 72f, cardTop + 64f, tagPaint)

        // Quote
        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = when {
                quote.length > 100 -> 44f
                else -> 52f
            }
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val textWidth = (cardWidth - 90f).toInt()
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(quote, 0, quote.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(14f, 1.25f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(quote, textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.25f, 14f, false)
        }

        canvas.save()
        canvas.translate(cardLeft + 45f, cardTop + 120f)
        staticLayout.draw(canvas)
        canvas.restore()

        // Author
        val authorPaint = Paint().apply {
            isAntiAlias = true
            color = highlightColor.toInt()
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("— $author", cardLeft + 45f, cardRect.bottom - 40f, authorPaint)
    }

    /**
     * Style 5: Glassmorphic Frost Card
     */
    private fun renderGlassmorphicCard(
        canvas: Canvas,
        width: Int,
        height: Int,
        quote: String,
        author: String,
        aspectRatio: String,
        highlightColor: Long = 0xFF7DD3FCL
    ) {
        val cardWidth = width * 0.88f
        val cardHeight = if (aspectRatio == "1:1") height * 0.72f else height * 0.50f
        val cardLeft = (width - cardWidth) / 2f
        val cardTop = (height - cardHeight) / 2f
        val cardRect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)

        val glassPaint = Paint().apply {
            isAntiAlias = true
            color = 0xCC101217.toInt()
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, glassPaint)

        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = highlightColor.toInt()
            alpha = 160
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

        // Quotation mark
        val qPaint = Paint().apply {
            isAntiAlias = true
            color = highlightColor.toInt()
            textSize = 90f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("“", width / 2f, cardTop + 100f, qPaint)

        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val padding = 90
        val textWidth = (cardWidth - padding * 2).toInt()
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(quote, 0, quote.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(16f, 1.25f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(quote, textPaint, textWidth, Layout.Alignment.ALIGN_CENTER, 1.25f, 16f, false)
        }

        val textY = cardTop + 160f
        canvas.save()
        canvas.translate(cardLeft + padding, textY)
        staticLayout.draw(canvas)
        canvas.restore()

        val authorPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            alpha = 220
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("— $author", width / 2f, cardRect.bottom - 45f, authorPaint)
    }

    private fun loadAssetBitmap(context: Context, assetPath: String, targetW: Int, targetH: Int): Bitmap? {
        return try {
            context.assets.open(assetPath).use { stream ->
                val orig = BitmapFactory.decodeStream(stream) ?: return null
                val srcW = orig.width
                val srcH = orig.height
                val scale = maxOf(targetW.toFloat() / srcW, targetH.toFloat() / srcH)
                val scaledW = (srcW * scale).toInt()
                val scaledH = (srcH * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(orig, scaledW, scaledH, true)
                val left = (scaledW - targetW) / 2
                val top = (scaledH - targetH) / 2
                Bitmap.createBitmap(scaled, left.coerceAtLeast(0), top.coerceAtLeast(0), targetW, targetH)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String = "ThoughtFlow_Quote"): Uri? {
        val filename = "${title}_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null
        var imageUri: Uri? = null

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ThoughtFlow")
                }
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/ThoughtFlow"
                val file = File(imagesDir)
                if (!file.exists()) file.mkdirs()
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
                imageUri = Uri.fromFile(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            imageUri
        } catch (e: Exception) {
            null
        }
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, thoughtText: String) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/shared_quote.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val newFile = File(cachePath, "shared_quote.png")
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "\"$thoughtText\"\n\nCreated with ThoughtFlow AI")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Quote Photo"))
            }
        } catch (e: Exception) {
            val textIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "\"$thoughtText\"\n\nCreated with ThoughtFlow AI")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(textIntent, "Share Quote"))
        }
    }
}
