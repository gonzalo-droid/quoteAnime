package com.gondroid.quoteanime.presentation.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.Quote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val SHARE_W = 1080
private const val SHARE_H = 1350        // 4:5 — Instagram portrait, WhatsApp, X

// Wider horizontal padding so the quote block looks more centered and airy
private const val H_PAD = 130f

/**
 * Generates a 1080×1350 (4:5) share image.
 * Background: [quote.imageUrl] loaded from Cloudinary when available,
 * otherwise a dark gradient.
 */
suspend fun createShareBitmap(quote: Quote, context: Context): Bitmap {
    val background = loadBackgroundBitmap(context, quote.imageUrl)
    val appIcon = loadAppIconBitmap(context)
    return withContext(Dispatchers.Default) {
        drawShareBitmap(quote, background, appIcon, context.getString(R.string.app_name_label))
    }
}

private suspend fun loadBackgroundBitmap(context: Context, imageUrl: String?): Bitmap? {
    if (imageUrl.isNullOrBlank()) return null
    return runCatching {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false) // Required to manipulate the bitmap on canvas
            .build()
        (loader.execute(request).drawable as? BitmapDrawable)?.bitmap
    }.getOrNull()
}

/**
 * Renders the round app icon into a bitmap.
 * Uses [ContextCompat.getDrawable] which handles WebP, adaptive icons, and XML drawables.
 * Falls back to a solid accent-colour circle so the watermark always has an icon.
 */
private fun loadAppIconBitmap(context: Context): Bitmap {
    val size = 160  // internal resolution — will be scaled down when drawn
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_app_logo_round)
    if (drawable != null) {
        drawable.setBounds(0, 0, size, size)
        drawable.draw(c)
    } else {
        // Fallback: filled circle in accent purple
        c.drawCircle(size / 2f, size / 2f, size / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFA78BFA.toInt() })
    }
    return bmp
}

private fun drawShareBitmap(
    quote: Quote,
    background: Bitmap?,
    appIcon: Bitmap,
    label: String
): Bitmap {
    val w = SHARE_W
    val h = SHARE_H
    val cx = w / 2f
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // ── Background ────────────────────────────────────────────────────────
    if (background != null) {
        drawCroppedBackground(canvas, background, w, h)
    } else {
        canvas.drawRect(
            0f, 0f, w.toFloat(), h.toFloat(),
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h.toFloat(),
                    intArrayOf(0xFF0C0C1E.toInt(), 0xFF1A0E2E.toInt()),
                    null, Shader.TileMode.CLAMP
                )
            }
        )
    }

    // ── Dark overlay (top light → center medium → bottom heavy) ──────────
    canvas.drawRect(
        0f, 0f, w.toFloat(), h.toFloat(),
        Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(
                    0x73000000.toInt(),  // ~45% top
                    0xB8000000.toInt(),  // ~72% center
                    0xEB000000.toInt()   // ~92% bottom
                ),
                null, Shader.TileMode.CLAMP
            )
        }
    )

    // ── Decorative opening quote mark ─────────────────────────────────────
    val quoteMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x38A78BFA.toInt()
        textSize = 140f
        typeface = Typeface.SERIF
    }
    canvas.drawText("\u201C", H_PAD - 12f, 220f, quoteMarkPaint)

    // ── Text paints ───────────────────────────────────────────────────────
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF0EAFF.toInt()
        textSize = 52f
        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        textAlign = Paint.Align.CENTER
    }
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCF0EAFF.toInt()
        textSize = 38f
        textAlign = Paint.Align.CENTER
    }
    val animePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCA78BFA.toInt()
        textSize = 26f
        letterSpacing = 0.18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val maxTextWidth = w - H_PAD * 2
    val lines = wrapText(quote.quote.orEmpty(), textPaint, maxTextWidth)
    val lineHeight = 70f

    // ── Layout constants ──────────────────────────────────────────────────
    val gapQuoteToDivider = 56f   // breathing room after last quote line
    val dividerWidth = 64f
    val dividerThickness = 1.5f
    val gapDividerToAuthor = 40f  // space between divider and author name
    val gapAuthorToAnime = 16f

    // Total content block height for vertical centering
    val hasAnime = quote.anime?.uppercase().orEmpty().isNotBlank()
    val contentHeight = (lines.size * lineHeight)
        .plus(gapQuoteToDivider)
        .plus(dividerThickness)
        .plus(gapDividerToAuthor)
        .plus(authorPaint.textSize)
        .let { if (hasAnime) it.plus(gapAuthorToAnime).plus(animePaint.textSize) else it }

    var y = (h - contentHeight) / 2f + textPaint.textSize
    if (y < 240f) y = 240f  // never overlap the decorative quote mark

    // ── Quote text lines ──────────────────────────────────────────────────
    lines.forEach { line ->
        canvas.drawText(line, cx, y, textPaint)
        y += lineHeight
    }

    // ── Divider ───────────────────────────────────────────────────────────
    y += gapQuoteToDivider
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55F0EAFF.toInt()
        strokeWidth = dividerThickness
        style = Paint.Style.STROKE
    }
    canvas.drawLine(cx - dividerWidth / 2f, y, cx + dividerWidth / 2f, y, dividerPaint)

    // ── Author ────────────────────────────────────────────────────────────
    y += gapDividerToAuthor + authorPaint.textSize
    canvas.drawText("— ${quote.author.orEmpty()}", cx, y, authorPaint)

    // ── Anime name ────────────────────────────────────────────────────────
    val animeName = quote.anime?.uppercase().orEmpty()
    if (animeName.isNotBlank()) {
        y += gapAuthorToAnime + animePaint.textSize
        canvas.drawText(animeName, cx, y, animePaint)
    }

    // ── Watermark: icon + app name (bottom-right corner) ─────────────────
    drawWatermark(canvas, appIcon, label, w, h)

    return bitmap
}

/**
 * Draws a pill watermark in the bottom-right corner: [appIcon] + [label].
 *
 * Layout (right-aligned):
 *   [ icon ]  App Name
 *
 * Everything is vertically centred inside the pill.
 */
private fun drawWatermark(
    canvas: Canvas,
    appIcon: Bitmap,
    label: String,
    w: Int,
    h: Int
) {
    val iconSize  = 56          // px drawn on the 1080-wide canvas
    val textSize  = 30f
    val iconTextGap = 16f
    val rightMargin = 52f
    val bottomMargin = 60f
    val pillPadH  = 18f         // horizontal padding inside the pill
    val pillPadV  = 14f         // vertical padding inside the pill

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = 0xE6FFFFFF.toInt()    // 90% white
        this.textSize  = textSize
        typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    val textWidth  = labelPaint.measureText(label)
    val pillW      = pillPadH + iconSize + iconTextGap + textWidth + pillPadH
    val pillH      = iconSize + pillPadV * 2

    // Pill rect — anchored to bottom-right
    val pillRight  = w - rightMargin
    val pillBottom = h - bottomMargin
    val pillLeft   = pillRight - pillW
    val pillTop    = pillBottom - pillH

    // Semi-transparent dark pill
    canvas.drawRoundRect(
        RectF(pillLeft, pillTop, pillRight, pillBottom),
        pillH / 2f, pillH / 2f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66000000.toInt() }
    )

    // Icon — vertically centred in the pill
    val iconLeft = pillLeft + pillPadH
    val iconTop  = pillTop + (pillH - iconSize) / 2f
    val scaledIcon = Bitmap.createScaledBitmap(appIcon, iconSize, iconSize, true)
    canvas.drawBitmap(scaledIcon, iconLeft, iconTop, Paint(Paint.FILTER_BITMAP_FLAG))

    // Label — baseline aligned to vertical centre of icon
    val textX = iconLeft + iconSize + iconTextGap
    // Paint baseline: centre of icon + half of cap-height (≈ 0.35 × textSize)
    val textY = iconTop + iconSize / 2f + textSize * 0.35f
    canvas.drawText(label, textX, textY, labelPaint)
}

/** Scale-and-crop [src] to fill a [targetW]×[targetH] canvas (center crop). */
private fun drawCroppedBackground(canvas: Canvas, src: Bitmap, targetW: Int, targetH: Int) {
    val srcW = src.width.toFloat()
    val srcH = src.height.toFloat()
    val scale = maxOf(targetW / srcW, targetH / srcH)
    val scaledW = srcW * scale
    val scaledH = srcH * scale
    val left = (targetW - scaledW) / 2f
    val top = (targetH - scaledH) / 2f
    canvas.drawBitmap(
        src, null,
        RectF(left, top, left + scaledW, top + scaledH),
        Paint(Paint.FILTER_BITMAP_FLAG)
    )
}

/** Wraps [text] into lines fitting within [maxWidth] pixels. */
@androidx.annotation.VisibleForTesting
internal fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var current = ""
    for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidth) {
            current = candidate
        } else {
            if (current.isNotEmpty()) lines.add(current)
            current = word
        }
    }
    if (current.isNotEmpty()) lines.add(current)
    return lines
}

/** Saves [bitmap] to cache and launches the system share chooser. */
fun shareQuoteAsBitmap(context: Context, bitmap: Bitmap) {
    val file = File(context.cacheDir, "quote_share.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            null
        )
    )
}
