package com.gondroid.quoteanime.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
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
import com.gondroid.quoteanime.R

class QuoteWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: android.content.Context, id: androidx.glance.GlanceId) {
        provideContent { Content() }
    }

    @Composable
    override fun Content() {
        val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
        val quoteText = prefs[QuoteWidgetState.QUOTE_TEXT]
        val quoteAuthor = prefs[QuoteWidgetState.QUOTE_AUTHOR]
        val isLoading = prefs[QuoteWidgetState.IS_LOADING] ?: true
        val hasError = prefs[QuoteWidgetState.HAS_ERROR] ?: false

        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .cornerRadius(16.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> LoadingContent()
                    hasError -> ErrorContent()
                    else -> QuoteContent(
                        text = quoteText ?: "",
                        author = quoteAuthor ?: ""
                    )
                }
            }
        }
    }

    @Composable
    private fun LoadingContent() {
        Text(
            text = "Cargando frase…",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 14.sp
            )
        )
    }

    @Composable
    private fun ErrorContent() {
        Column(
            modifier = GlanceModifier.fillMaxWidth().wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No se pudo cargar la frase.",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            RefreshButton()
        }
    }

    @Composable
    private fun QuoteContent(text: String, author: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u201C$text\u201D",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic
                ),
                maxLines = 5,
                modifier = GlanceModifier.fillMaxWidth()
            )

            if (author.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = "— $author",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(12.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                RefreshButton()
            }
        }
    }

    @Composable
    private fun RefreshButton() {
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = "Actualizar frase",
            modifier = GlanceModifier
                .size(24.dp)
                .clickable(actionRunCallback<RefreshQuoteAction>())
        )
    }
}
