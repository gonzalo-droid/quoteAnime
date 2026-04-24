package com.gondroid.quoteanime.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import com.gondroid.quoteanime.ui.theme.Didot
import com.gondroid.quoteanime.ui.theme.Georgia
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.ui.theme.HeartRed
import androidx.compose.ui.tooling.preview.Preview
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme

@Composable
fun QuoteCard(
    quote: Quote,
    onToggleFavorite: (Quote) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = 4
) {
    val heartScale by animateFloatAsState(
        targetValue = if (quote.isFavorite) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "heartScale"
    )
    val heartTint by animateColorAsState(
        targetValue = if (quote.isFavorite) HeartRed
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "heartTint"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = quote.quote.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = Georgia,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis
                )

                if (!quote.author.isNullOrBlank()) {
                    Text(
                        text = "— ${quote.author}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = Didot,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!quote.anime.isNullOrBlank()) {
                    Text(
                        text = quote.anime.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
            }

            IconButton(onClick = { onToggleFavorite(quote) }) {
                Icon(
                    imageVector = if (quote.isFavorite) Icons.Default.Favorite
                                  else Icons.Default.FavoriteBorder,
                    contentDescription = if (quote.isFavorite) "Quitar de favoritos"
                                         else "Añadir a favoritos",
                    tint = heartTint,
                    modifier = Modifier
                        .size(22.dp)
                        .scale(heartScale)
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Card — normal", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PreviewQuoteCardNormal() {
    QuoteAnimeTheme {
        QuoteCard(
            quote = Quote(
                id = "1",
                quote = "El coraje no es la ausencia del miedo, sino el juicio de que algo más es importante que el miedo.",
                author = "Izuku Midoriya",
                anime = "Boku no Hero Academia"
            ),
            onToggleFavorite = {},
            modifier = androidx.compose.ui.Modifier.padding(12.dp)
        )
    }
}

@Preview(name = "Card — favorita", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PreviewQuoteCardFavorite() {
    QuoteAnimeTheme {
        QuoteCard(
            quote = Quote(
                id = "2",
                quote = "No me rindo. Nunca me rendiré. Ese es mi camino ninja.",
                author = "Naruto Uzumaki",
                anime = "Naruto",
                isFavorite = true
            ),
            onToggleFavorite = {},
            modifier = androidx.compose.ui.Modifier.padding(12.dp)
        )
    }
}

@Preview(name = "Card — texto largo (truncado)", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PreviewQuoteCardLong() {
    QuoteAnimeTheme {
        QuoteCard(
            quote = Quote(
                id = "3",
                quote = "La gente vive basándose en su imagen de sí misma. Eso limita a las personas. Por eso los errores duelen. Pero entender los errores te lleva al siguiente paso. No dejes que tus errores te detengan. No tengas miedo a equivocarte.",
                author = "Jiraiya",
                anime = "Naruto Shippuden"
            ),
            onToggleFavorite = {},
            modifier = androidx.compose.ui.Modifier.padding(12.dp)
        )
    }
}

@Preview(name = "Card — sin autor ni anime", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PreviewQuoteCardMinimal() {
    QuoteAnimeTheme {
        QuoteCard(
            quote = Quote(
                id = "4",
                quote = "Incluso si somos insectos, vivimos.",
                author = null,
                anime = null
            ),
            onToggleFavorite = {},
            modifier = androidx.compose.ui.Modifier.padding(12.dp)
        )
    }
}
