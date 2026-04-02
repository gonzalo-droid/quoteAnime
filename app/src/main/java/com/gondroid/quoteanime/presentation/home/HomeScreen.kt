package com.gondroid.quoteanime.presentation.home

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.domain.model.Quote
import kotlin.math.absoluteValue

// ── Palette ──────────────────────────────────────────────────────────────────
private val BgDark        = Color(0xFF0C0C1E)
private val TextPrimary   = Color(0xFFF0EAFF)
private val TextSecondary = Color(0xFF9B8DB3)
private val AccentPurple  = Color(0xFFA78BFA)
private val HeartColor    = Color(0xFFFF6B8A)
private val DividerColor  = Color(0xFF3A3050)

private val pageGradients = listOf(
    listOf(Color(0xFF0C0C1E), Color(0xFF1A0E2E)),
    listOf(Color(0xFF0A1020), Color(0xFF0E1E3A)),
    listOf(Color(0xFF12080E), Color(0xFF260A20)),
    listOf(Color(0xFF080C14), Color(0xFF0C1A2C)),
    listOf(Color(0xFF100818), Color(0xFF1E0C30)),
)

// ── Entry point ──────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    onNavigateToCatalog: (categoryId: String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    color = AccentPurple,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.error != null || uiState.quotes.isEmpty() -> {
                Text(
                    text = if (uiState.error != null) "No se pudo cargar las frases."
                           else "Sin frases disponibles.",
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                val pagerState = rememberPagerState(pageCount = { uiState.quotes.size })
                val currentQuote = uiState.quotes[pagerState.currentPage]

                // ── Pager: solo el contenido de la frase scrollea ────────────
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        // deja espacio inferior para los botones fijos
                        .padding(bottom = 120.dp)
                ) { page ->
                    val quote = uiState.quotes[page]
                    val pageOffset = ((pagerState.currentPage - page) +
                            pagerState.currentPageOffsetFraction).absoluteValue

                    QuoteContent(
                        quote = quote,
                        page = page,
                        pageOffset = pageOffset
                    )
                }

                // ── Settings — top right (fijo) ──────────────────────────────
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Ajustes",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // ── Contador de página — top left (fijo) ─────────────────────
                Text(
                    text = "${pagerState.currentPage + 1} / ${uiState.quotes.size}",
                    color = TextSecondary.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 20.dp, top = 16.dp)
                )

                // ── Botones de acción — FIJOS en la parte inferior ───────────
                BottomActions(
                    quote = currentQuote,
                    onToggleFavorite = { viewModel.onToggleFavorite(currentQuote) },
                    onShare = {
                        val shareText = "\"${currentQuote.quote}\"\n— ${currentQuote.author}\n(${currentQuote.anime})"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                    onNavigateToCatalog = { onNavigateToCatalog(null) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 28.dp)
                )
            }
        }
    }
}

// ── Solo el contenido de la frase (scrollea con el pager) ────────────────────
@Composable
private fun QuoteContent(
    quote: Quote,
    page: Int,
    pageOffset: Float
) {
    val gradient = pageGradients[page % pageGradients.size]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradient))
            .graphicsLayer {
                alpha = 1f - (pageOffset * 0.4f).coerceIn(0f, 0.4f)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Comilla decorativa
            Text(
                text = "\u201C",
                fontSize = 96.sp,
                color = AccentPurple.copy(alpha = 0.18f),
                fontFamily = FontFamily.Serif,
                lineHeight = 60.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(start = 4.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = quote.quote,
                fontSize = 22.sp,
                lineHeight = 34.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            HorizontalDivider(
                modifier = Modifier.width(48.dp),
                color = DividerColor,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "— ${quote.author}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = quote.anime.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = AccentPurple.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Botones fijos ─────────────────────────────────────────────────────────────
@Composable
private fun BottomActions(
    quote: Quote,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onNavigateToCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heartScale by animateFloatAsState(
        targetValue = if (quote.isFavorite) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "heartScale"
    )
    val heartTint by animateColorAsState(
        targetValue = if (quote.isFavorite) HeartColor else TextSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "heartTint"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (quote.isFavorite) Icons.Default.Favorite
                              else Icons.Default.FavoriteBorder,
                contentDescription = if (quote.isFavorite) "Quitar favorito" else "Añadir favorito",
                tint = heartTint,
                modifier = Modifier
                    .size(24.dp)
                    .scale(heartScale)
            )
        }

        ActionButton(onClick = onShare) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Compartir",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        ActionButton(onClick = onNavigateToCatalog) {
            Icon(
                imageVector = Icons.Outlined.GridView,
                contentDescription = "Explorar",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ActionButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            content()
        }
    }
}
