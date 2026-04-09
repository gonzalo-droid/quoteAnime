package com.gondroid.quoteanime.presentation.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.gondroid.quoteanime.domain.model.Quote
import java.io.File

/**
 * Generates a share image for a quote using Android Canvas.
 * Card dimensions: 320 x 420 dp (scaled to pixels by [density]).
 */
fun createShareBitmap(quote: Quote, density: Float): Bitmap {
    val w = (320 * density).toInt()
    val h = (420 * density).toInt()
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // ── Background gradient ────────────────────────────────────────────────
    val bgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0xFF0C0C1E.toInt(), 0xFF1A0E2E.toInt()),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

    val cx = w / 2f
    val hPad = 32 * density

    // ── Decorative opening quote mark ──────────────────────────────────────
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x38A78BFA.toInt()  // AccentPurple ~22% alpha
        textSize = 72 * density
        typeface = Typeface.SERIF
    }
    canvas.drawText("\u201C", hPad - 4 * density, 100 * density, quotePaint)

    // ── Quote text (centered, word-wrapped) ────────────────────────────────
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF0EAFF.toInt()  // TextPrimary
        textSize = 17 * density
        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        textAlign = Paint.Align.CENTER
    }
    val maxTextWidth = w - hPad * 2
    val lines = wrapText(quote.quote.orEmpty(), textPaint, maxTextWidth)
    val lineHeight = 27 * density
    var y = 128 * density
    lines.forEach { line ->
        canvas.drawText(line, cx, y, textPaint)
        y += lineHeight
    }

    // ── Divider ────────────────────────────────────────────────────────────
    y += 20 * density
    val divPaint = Paint().apply {
        color = 0xFF4A4270.toInt()  // OutlineColor
        strokeWidth = density
    }
    canvas.drawLine(cx - 20 * density, y, cx + 20 * density, y, divPaint)
    y += 20 * density

    // ── Author ─────────────────────────────────────────────────────────────
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xBFF0EAFF.toInt()  // TextPrimary ~75%
        textSize = 13 * density
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("— ${quote.author.orEmpty()}", cx, y, authorPaint)
    y += 18 * density

    // ── Anime name ─────────────────────────────────────────────────────────
    val animePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCA78BFA.toInt()  // AccentPurple 80%
        textSize = 9 * density
        letterSpacing = 0.18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(quote.anime?.uppercase().orEmpty(), cx, y, animePaint)

    // ── Watermark ──────────────────────────────────────────────────────────
    val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x809B8DB3.toInt()  // TextSecondary 50%
        textSize = 9 * density
        letterSpacing = 0.1f
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("Quote Anime", w - 14 * density, h - 14 * density, wmPaint)

    return bitmap
}

/** Wraps [text] into lines that fit within [maxWidth] pixels. */
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

/** Saves [bitmap] to cache and launches a share chooser. */
fun shareQuoteAsBitmap(context: Context, bitmap: Bitmap) {
    val file = File(context.cacheDir, "quote_share.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
