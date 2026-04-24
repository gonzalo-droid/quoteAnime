package com.gondroid.quoteanime.widget

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
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

// ── Colors (hardcoded for Glance — no MaterialTheme available) ───────────────
@SuppressLint("RestrictedApi")
private val ColorTextPrimary   = ColorProvider(androidx.compose.ui.graphics.Color(0xFFF0EAFF))
@SuppressLint("RestrictedApi")
private val ColorTextSecondary = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9B8DB3))
@SuppressLint("RestrictedApi")
private val ColorAccent        = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA78BFA))

// ── Size breakpoints ──────────────────────────────────────────────────────────
private val SIZE_SMALL  = DpSize(110.dp,  80.dp)
private val SIZE_MEDIUM = DpSize(180.dp, 120.dp)
private val SIZE_LARGE  = DpSize(250.dp, 180.dp)

class QuoteWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    // Responsive: Glance renders the right layout based on the actual physical dimensions
    override val sizeMode = SizeMode.Responsive(setOf(SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE))

    override suspend fun provideGlance(
        context: android.content.Context,
        id: androidx.glance.GlanceId
    ) {
        provideContent { Content() }
    }

    @Composable
    fun Content() {
        val prefs       = currentState<androidx.datastore.preferences.core.Preferences>()
        val quoteText   = prefs[QuoteWidgetState.QUOTE_TEXT]
        val quoteAuthor = prefs[QuoteWidgetState.QUOTE_AUTHOR]
        val quoteId     = prefs[QuoteWidgetState.QUOTE_ID]     ?: ""
        val quoteAnime  = prefs[QuoteWidgetState.QUOTE_ANIME]  ?: ""
        val isLoading   = prefs[QuoteWidgetState.IS_LOADING]   ?: true
        val hasError    = prefs[QuoteWidgetState.HAS_ERROR]    ?: false

        // Physical size reported by the launcher
        val size = LocalSize.current

        val context = LocalContext.current
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            putExtra("widget_quote_id", quoteId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val isSmall  = size.width < SIZE_MEDIUM.width
        val isLarge  = size.width >= SIZE_LARGE.width && size.height >= SIZE_LARGE.height

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_bg))
                .cornerRadius(20.dp)
                .clickable(actionStartActivity(openIntent))
                .padding(
                    horizontal = if (isSmall) 12.dp else 16.dp,
                    vertical   = if (isSmall) 10.dp else 14.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> LoadingContent()
                hasError  -> ErrorContent()
                isLarge   -> LargeQuoteContent(
                    text   = quoteText  ?: "",
                    author = quoteAuthor ?: "",
                    anime  = quoteAnime
                )
                isSmall   -> SmallQuoteContent(text = quoteText ?: "")
                else      -> MediumQuoteContent(
                    text   = quoteText  ?: "",
                    author = quoteAuthor ?: ""
                )
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
        }
    }

    // ── SMALL (< 180dp ancho): solo la frase ─────────────────────────────────
    @Composable
    private fun SmallQuoteContent(text: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = TextStyle(
                    color     = ColorTextPrimary,
                    fontSize  = 12.sp,
                    fontStyle = FontStyle.Italic
                ),
                maxLines = 3,
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }

    // ── MEDIUM (180–249dp ancho): frase + autor ───────────────────────────────
    @Composable
    private fun MediumQuoteContent(text: String, author: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
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
        }
    }

    // ── LARGE (≥ 250dp ancho y ≥ 180dp alto): frase + autor + anime ──────────
    @Composable
    private fun LargeQuoteContent(text: String, author: String, anime: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    style = TextStyle(color = ColorAccent, fontSize = 9.sp)
                )
            }
        }
    }
}
