package com.gondroid.quoteanime.widget

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gondroid.quoteanime.MainActivity
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.WidgetSize

// ── Colors (hardcoded for Glance — no MaterialTheme available) ───────────────
@SuppressLint("RestrictedApi")
private val ColorTextPrimary   = ColorProvider(androidx.compose.ui.graphics.Color(0xFFF0EAFF))
@SuppressLint("RestrictedApi")
private val ColorTextSecondary = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9B8DB3))
@SuppressLint("RestrictedApi")
private val ColorAccent        = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA78BFA))

class QuoteWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(
        context: android.content.Context,
        id: androidx.glance.GlanceId
    ) {
        provideContent { Content() }
    }

    @Composable
    fun Content() {
        val prefs      = currentState<androidx.datastore.preferences.core.Preferences>()
        val quoteText  = prefs[QuoteWidgetState.QUOTE_TEXT]
        val quoteAuthor = prefs[QuoteWidgetState.QUOTE_AUTHOR]
        val quoteId    = prefs[QuoteWidgetState.QUOTE_ID] ?: ""
        val quoteAnime = prefs[QuoteWidgetState.QUOTE_ANIME] ?: ""
        val isLoading  = prefs[QuoteWidgetState.IS_LOADING] ?: true
        val hasError   = prefs[QuoteWidgetState.HAS_ERROR] ?: false
        val widgetSize = prefs[QuoteWidgetState.WIDGET_SIZE]
            ?.let { runCatching { WidgetSize.valueOf(it) }.getOrNull() }
            ?: WidgetSize.MEDIUM

        val context = LocalContext.current
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            putExtra("widget_quote_id", quoteId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_bg))
                .cornerRadius(20.dp)
                .clickable(actionStartActivity(openIntent))
                .padding(
                    horizontal = if (widgetSize == WidgetSize.SMALL) 12.dp else 16.dp,
                    vertical   = if (widgetSize == WidgetSize.SMALL) 10.dp else 14.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> LoadingContent()
                hasError  -> ErrorContent()
                else -> when (widgetSize) {
                    WidgetSize.SMALL  -> SmallQuoteContent(text = quoteText ?: "")
                    WidgetSize.MEDIUM -> MediumQuoteContent(
                        text   = quoteText  ?: "",
                        author = quoteAuthor ?: ""
                    )
                    WidgetSize.LARGE  -> LargeQuoteContent(
                        text   = quoteText  ?: "",
                        author = quoteAuthor ?: "",
                        anime  = quoteAnime
                    )
                }
            }
        }
    }

    // ── Loading ──────────────────────────────────────────────────────────────
    @Composable
    private fun LoadingContent() {
        Text(
            text = "Cargando frase…",
            style = TextStyle(color = ColorTextSecondary, fontSize = 13.sp)
        )
    }

    // ── Error ────────────────────────────────────────────────────────────────
    @Composable
    private fun ErrorContent() {
        Column(
            modifier = GlanceModifier.fillMaxWidth().wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No se pudo cargar la frase.",
                style = TextStyle(color = ColorTextSecondary, fontSize = 12.sp)
            )
            Spacer(GlanceModifier.height(8.dp))
            RefreshButton()
        }
    }

    // ── SMALL: solo la frase, tipografía mediana ─────────────────────────────
    @Composable
    private fun SmallQuoteContent(text: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u201C$text\u201D",
                style = TextStyle(
                    color     = ColorTextPrimary,
                    fontSize  = 12.sp,
                    fontStyle = FontStyle.Italic
                ),
                maxLines = 3,
                modifier = GlanceModifier.fillMaxWidth()
            )
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) { RefreshButton() }
        }
    }

    // ── MEDIUM: frase + autor ────────────────────────────────────────────────
    @Composable
    private fun MediumQuoteContent(text: String, author: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u201C$text\u201D",
                style = TextStyle(
                    color     = ColorTextPrimary,
                    fontSize  = 14.sp,
                    fontStyle = FontStyle.Italic
                ),
                maxLines = 4,
                modifier = GlanceModifier.fillMaxWidth()
            )
            if (author.isNotBlank()) {
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text  = "— $author",
                    style = TextStyle(
                        color      = ColorTextSecondary,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Spacer(GlanceModifier.height(10.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) { RefreshButton() }
        }
    }

    // ── LARGE: frase + autor + anime ─────────────────────────────────────────
    @Composable
    private fun LargeQuoteContent(text: String, author: String, anime: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decorative mark
            Text(
                text  = "\u201C",
                style = TextStyle(color = ColorAccent, fontSize = 36.sp),
                modifier = GlanceModifier.fillMaxWidth()
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = text,
                style = TextStyle(
                    color     = ColorTextPrimary,
                    fontSize  = 15.sp,
                    fontStyle = FontStyle.Italic
                ),
                maxLines = 6,
                modifier = GlanceModifier.fillMaxWidth()
            )
            if (author.isNotBlank()) {
                Spacer(GlanceModifier.height(10.dp))
                Text(
                    text  = "— $author",
                    style = TextStyle(
                        color      = ColorTextSecondary,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            if (anime.isNotBlank()) {
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text  = anime.uppercase(),
                    style = TextStyle(
                        color    = ColorAccent,
                        fontSize = 9.sp
                    )
                )
            }
            Spacer(GlanceModifier.height(10.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) { RefreshButton() }
        }
    }

    @Composable
    private fun RefreshButton() {
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = "Actualizar",
            modifier = GlanceModifier
                .size(22.dp)
                .clickable(actionRunCallback<RefreshQuoteAction>())
        )
    }
}
