package com.gondroid.quoteanime.presentation.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.presentation.components.QuoteCard
import com.gondroid.quoteanime.presentation.home.createShareBitmap
import com.gondroid.quoteanime.presentation.home.shareQuoteAsBitmap
import com.gondroid.quoteanime.ui.theme.AccentPurple
import com.gondroid.quoteanime.ui.theme.HeartRed
import com.gondroid.quoteanime.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Emotion catalog definitions ───────────────────────────────────────────────

private data class EmotionOption(
    val categoryId: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

private val emotions = listOf(
    EmotionOption("motivación",  "Motivación",  Icons.Default.Bolt,            Color(0xFFFF8F00)),
    EmotionOption("lucha",       "Lucha",        Icons.Default.Shield,           Color(0xFFD32F2F)),
    EmotionOption("tristeza",    "Tristeza",     Icons.Default.WaterDrop,        Color(0xFF546E7A)),
    EmotionOption("amor",        "Amor",         Icons.Default.Favorite,         Color(0xFFE91E63)),
    EmotionOption("amistad",     "Amistad",      Icons.Default.People,           Color(0xFF388E3C)),
    EmotionOption("reflexión",   "Reflexión",    Icons.Default.Psychology,       Color(0xFF4527A0)),
    EmotionOption("soledad",     "Soledad",      Icons.Default.Nightlight,       Color(0xFF37474F)),
    EmotionOption("sacrificio",  "Sacrificio",   Icons.Default.SelfImprovement,  Color(0xFF880E4F)),
    EmotionOption("esperanza",   "Esperanza",    Icons.Default.WbSunny,          Color(0xFFF57F17)),
    EmotionOption("orgullo",     "Orgullo",      Icons.Default.EmojiEvents,      Color(0xFFAD8000))
)

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun CatalogScreen(
    onNavigateBack: () -> Unit,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = context.resources.displayMetrics.density

    // Internal back navigation (Detail → List → Selector)
    BackHandler(enabled = uiState.selectedQuote != null) { viewModel.onBackFromDetail() }
    BackHandler(enabled = uiState.selectedQuote == null && uiState.selectedFilter != null) {
        viewModel.onBackFromList()
    }

    when {
        uiState.selectedQuote != null -> {
            QuoteDetailContent(
                quote = uiState.selectedQuote!!,
                onBack = { viewModel.onBackFromDetail() },
                onToggleFavorite = { viewModel.onToggleFavorite(it) },
                onShare = { quote ->
                    scope.launch {
                        val bitmap = withContext(Dispatchers.Default) { createShareBitmap(quote, density) }
                        shareQuoteAsBitmap(context, bitmap)
                    }
                }
            )
        }

        uiState.selectedFilter != null -> {
            CatalogListContent(
                filterLabel = uiState.selectedFilter!!.label,
                quotes = uiState.quotes,
                isLoading = uiState.isLoading,
                isEmpty = uiState.isEmpty,
                onBack = { viewModel.onBackFromList() },
                onQuoteSelected = { viewModel.onQuoteSelected(it) },
                onToggleFavorite = { viewModel.onToggleFavorite(it) }
            )
        }

        else -> {
            CatalogSelectorContent(
                onNavigateBack = onNavigateBack,
                onFilterSelected = { viewModel.onFilterSelected(it) }
            )
        }
    }
}

// ── 1. Selector (hub) ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogSelectorContent(
    onNavigateBack: () -> Unit,
    onFilterSelected: (CatalogFilter) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Explorar",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Primary actions row: Favorites + All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryFilterCard(
                    label = "Favoritos",
                    icon = Icons.Default.Favorite,
                    color = AccentPurple,
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterSelected(CatalogFilter.Favorites) }
                )
                PrimaryFilterCard(
                    label = "Todas",
                    icon = Icons.Default.GridView,
                    color = Color(0xFF42A5F5),
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterSelected(CatalogFilter.All) }
                )
            }

            // Section divider
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Por emoción",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            // Emotion grid (2 columns)
            emotions.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { emotion ->
                        EmotionCard(
                            emotion = emotion,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onFilterSelected(
                                    CatalogFilter.ByEmotion(emotion.categoryId, emotion.label)
                                )
                            }
                        )
                    }
                    // Pad odd rows
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrimaryFilterCard(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun EmotionCard(
    emotion: EmotionOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(88.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = emotion.color.copy(alpha = 0.11f)),
        border = BorderStroke(1.dp, emotion.color.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = emotion.icon,
                contentDescription = null,
                tint = emotion.color,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = emotion.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── 2. Quote list ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogListContent(
    filterLabel: String,
    quotes: List<Quote>,
    isLoading: Boolean,
    isEmpty: Boolean,
    onBack: () -> Unit,
    onQuoteSelected: (Quote) -> Unit,
    onToggleFavorite: (Quote) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        filterLabel,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            isEmpty -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay frases disponibles",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(quotes, key = { it.id }) { quote ->
                        QuoteCard(
                            quote = quote,
                            onToggleFavorite = onToggleFavorite,
                            modifier = Modifier.clickable { onQuoteSelected(quote) }
                        )
                    }
                }
            }
        }
    }
}

// ── 3. Full-screen quote detail ───────────────────────────────────────────────

@Composable
private fun QuoteDetailContent(
    quote: Quote,
    onBack: () -> Unit,
    onToggleFavorite: (Quote) -> Unit,
    onShare: (Quote) -> Unit
) {
    val context = LocalContext.current

    val heartScale by animateFloatAsState(
        targetValue = if (quote.isFavorite) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "heartScale"
    )
    val heartTint by animateColorAsState(
        targetValue = if (quote.isFavorite) HeartRed else Color.White.copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = 200),
        label = "heartTint"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Background image (Cloudinary or fallback drawable) ────────────────
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(
                    quote.imageUrl.takeIf { !it.isNullOrBlank() }
                        ?: R.drawable.onboarding_02
                )
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            error = painterResource(R.drawable.onboarding_02),
            fallback = painterResource(R.drawable.onboarding_02)
        )

        // ── Dark overlay ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.60f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // ── Back button ───────────────────────────────────────────────────────
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }

        // ── Quote content ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Decorative opening quote mark
            Text(
                text = "\u201C",
                fontSize = 96.sp,
                color = Color.White.copy(alpha = 0.20f),
                fontFamily = FontFamily.Serif,
                lineHeight = 60.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = quote.quote.orEmpty(),
                fontSize = 22.sp,
                lineHeight = 34.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider(
                modifier = Modifier.width(48.dp),
                color = Color.White.copy(alpha = 0.35f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "— ${quote.author.orEmpty()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.80f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = quote.anime?.uppercase().orEmpty(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = AccentPurple.copy(alpha = 0.90f),
                textAlign = TextAlign.Center
            )
        }

        // ── Bottom action buttons ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DetailActionButton(onClick = { onToggleFavorite(quote) }) {
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
            DetailActionButton(onClick = { onShare(quote) }) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Compartir",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailActionButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) { content() }
    }
}
