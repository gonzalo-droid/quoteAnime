package com.gondroid.quoteanime.presentation.home

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.presentation.ads.ShareInterstitialManager
import com.gondroid.quoteanime.presentation.components.QuoteDetailContent
import com.gondroid.quoteanime.ui.theme.AccentPurple
import com.gondroid.quoteanime.ui.theme.HeartRed
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import com.gondroid.quoteanime.ui.theme.TextSecondary
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

// BannerAd comentado temporalmente — reemplazado por interstitial en el flujo de compartir
// import com.gondroid.quoteanime.presentation.components.BannerAd

private val HeartColor = HeartRed

@Composable
fun HomeScreen(
    onNavigateToCatalog: (categoryId: String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRoutine: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val adManager = viewModel.shareInterstitialManager

    // Pre-cargar el interstitial en cuanto se abre HomeScreen
    LaunchedEffect(Unit) { adManager.preload(context) }

    HomeContent(
        uiState = uiState,
        onNavigateToCatalog = onNavigateToCatalog,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToRoutine = onNavigateToRoutine,
        onToggleFavorite = { viewModel.onToggleFavorite(it) },
        onScrollConsumed = { viewModel.onScrollToPageConsumed() },
        onShare = { quote ->
            val doShare = {
                scope.launch {
                    val bitmap = createShareBitmap(quote, context)
                    shareQuoteAsBitmap(context, bitmap)
                }
                Unit
            }
            if (activity != null) adManager.onShareRequested(activity, doShare)
            else doShare()
        }
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onNavigateToCatalog: (categoryId: String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRoutine: () -> Unit,
    onToggleFavorite: (quote: Quote) -> Unit,
    onScrollConsumed: () -> Unit,
    onShare: (quote: Quote) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
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

                // Scroll to widget quote when launched from widget tap
                LaunchedEffect(uiState.scrollToPage) {
                    val page = uiState.scrollToPage
                    if (page != null && page in uiState.quotes.indices) {
                        pagerState.animateScrollToPage(page)
                        onScrollConsumed()
                    }
                }

                // No navigationBarsPadding here on purpose: the background image is meant
                // to run edge-to-edge behind the system bars, same as the very first version
                // of this screen — only the floating controls below inset themselves.
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val quote = uiState.quotes[page]
                    val pageOffset = ((pagerState.currentPage - page) +
                            pagerState.currentPageOffsetFraction).absoluteValue

                    QuoteContent(
                        quote = quote,
                        pageOffset = pageOffset
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                BottomActions(
                    quote = currentQuote,
                    onToggleFavorite = { onToggleFavorite(currentQuote) },
                    onShare = { onShare(currentQuote) },
                    onNavigateToCatalog = { onNavigateToCatalog(null) },
                    onNavigateToRoutine = onNavigateToRoutine,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                )
            }
        }
    }
}

// ── Solo el contenido de la frase (scrollea con el pager) ────────────────────
@Composable
private fun QuoteContent(
    quote: Quote,
    pageOffset: Float
) {
    QuoteDetailContent(
        quote = quote,
        modifier = Modifier.graphicsLayer {
            alpha = 1f - (pageOffset * 0.4f).coerceIn(0f, 0.4f)
        }
        // onBack = null → no back button in pager
        // actions = empty default → bottom actions rendered outside the pager
    )
}

@Composable
private fun BottomActions(
    quote: Quote,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onNavigateToCatalog: () -> Unit,
    onNavigateToRoutine: () -> Unit,
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

    // Catálogo and Mi rutina anchor the row's edges as quick-access shortcuts;
    // favorite/share stay paired in the center as the quote's own actions.
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(onClick = onNavigateToCatalog) {
            Icon(
                imageVector = Icons.Outlined.GridView,
                contentDescription = stringResource(R.string.explore),
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (quote.isFavorite) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = if (quote.isFavorite) stringResource(R.string.remove_favorite)
                    else stringResource(R.string.add_favorite),
                    tint = heartTint,
                    modifier = Modifier.size(24.dp).scale(heartScale)
                )
            }

            ActionButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.action_shared),
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        ActionButton(onClick = onNavigateToRoutine) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = stringResource(R.string.nav_routine),
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
        IconButton(onClick = onClick) { content() }
    }
}

@Preview
@Composable
fun PreviewHomeContent() {
    QuoteAnimeTheme {
        HomeContent(
            uiState = HomeUiState(
                isLoading = false,
                quotes = listOf(
                    Quote(id = "1", quote = "Quote 1", author = "Author 1", anime = "Anime 1")
                )
            ),
            onNavigateToCatalog = {},
            onNavigateToSettings = {},
            onNavigateToRoutine = {},
            onToggleFavorite = {},
            onScrollConsumed = {},
            onShare = {}
        )
    }
}

@Preview(name = "Home — cargando")
@Composable
private fun PreviewHomeLoading() {
    QuoteAnimeTheme {
        HomeContent(
            uiState = HomeUiState(isLoading = true),
            onNavigateToCatalog = {},
            onNavigateToSettings = {},
            onNavigateToRoutine = {},
            onToggleFavorite = {},
            onScrollConsumed = {},
            onShare = {}
        )
    }
}

@Preview(name = "Home — con frases", showSystemUi = true)
@Composable
private fun PreviewHomeWithQuotes() {
    QuoteAnimeTheme {
        HomeContent(
            uiState = HomeUiState(
                isLoading = false,
                quotes = listOf(
                    Quote(
                        id = "1",
                        quote = "No me rindo. Nunca me rendiré. Ese es mi camino ninja.",
                        author = "Naruto Uzumaki",
                        anime = "Naruto",
                        isFavorite = true
                    ),
                    Quote(
                        id = "2",
                        quote = "El poder de la amistad es el más fuerte que existe.",
                        author = "Monkey D. Luffy",
                        anime = "One Piece"
                    )
                )
            ),
            onNavigateToCatalog = {},
            onNavigateToSettings = {},
            onNavigateToRoutine = {},
            onToggleFavorite = {},
            onScrollConsumed = {},
            onShare = {}
        )
    }
}
