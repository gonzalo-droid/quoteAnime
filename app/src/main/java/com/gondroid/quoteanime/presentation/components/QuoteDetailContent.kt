package com.gondroid.quoteanime.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.ui.theme.AccentPurple

/**
 * Full-screen quote detail composable shared by HomeScreen and CatalogScreen.
 *
 * @param quote      The quote to display.
 * @param onBack     When non-null, a back arrow is shown in the top-start corner.
 *                   Pass null (default) to hide it (HomeScreen pager case).
 * @param modifier   Modifier applied to the root Box.
 * @param actions    Optional bottom-area slot rendered inside the Box at BottomCenter.
 *                   Use this to place action buttons (Favorite, Share, etc.).
 */
@Composable
fun QuoteDetailContent(
    quote: Quote,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable (BoxScope.() -> Unit) = {}
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {

        // ── Background: image from Cloudinary, or fallback drawable ──────────
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

        // ── Dark gradient overlay for text legibility ─────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // ── Optional back button (top-start) ──────────────────────────────────
        if (onBack != null) {
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
        }

        // ── Quote text block (centered) ───────────────────────────────────────
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
                modifier = Modifier.fillMaxWidth(0.2f),
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

            val animeName = quote.anime?.uppercase().orEmpty()
            if (animeName.isNotBlank()) {
                Text(
                    text = animeName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = AccentPurple.copy(alpha = 0.90f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Bottom actions slot ───────────────────────────────────────────────
        actions()
    }
}

/**
 * Circular icon button used in the detail view actions row.
 * Shared between HomeScreen and CatalogScreen.
 */
@Composable
fun DetailActionButton(onClick: () -> Unit, content: @Composable () -> Unit) {
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
