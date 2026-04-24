package com.gondroid.quoteanime.presentation.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.ui.theme.AccentPurple
import com.gondroid.quoteanime.ui.theme.AccentPurpleDim
import com.gondroid.quoteanime.ui.theme.TextPrimary
import com.gondroid.quoteanime.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme

private data class OnboardingPage(
    val quote: String,
    @DrawableRes val imageRes: Int,
    val overlayGradient: List<Color>
)

private val pages = listOf(
    OnboardingPage(
        quote = "No me rindo. \nNunca me rendiré. \nEse es mi camino ninja.",
        imageRes = R.drawable.onboarding_01,
        overlayGradient = listOf(
            Color(0xFF0C0C1E).copy(alpha = 0.55f),
            Color(0xFF1A0E2E).copy(alpha = 0.75f),
            Color(0xFF0C0C1E).copy(alpha = 0.95f)
        )
    ),
    OnboardingPage(
        quote = "Un hombre que abandona sus sueños no es más que un cadáver.",
        imageRes = R.drawable.onboarding_02,
        overlayGradient = listOf(
            Color(0xFF0A1020).copy(alpha = 0.55f),
            Color(0xFF0E1E3A).copy(alpha = 0.75f),
            Color(0xFF0A1020).copy(alpha = 0.95f)
        )
    ),
    OnboardingPage(
        quote = "No importa cuánto poder tengas. Si lo usas para lastimar a otros, no eres mejor que ellos.",
        imageRes = R.drawable.onboarding_03,
        overlayGradient = listOf(
            Color(0xFF12080E).copy(alpha = 0.55f),
            Color(0xFF260A20).copy(alpha = 0.75f),
            Color(0xFF12080E).copy(alpha = 0.95f)
        )
    )
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    fun finish() = viewModel.onOnboardingFinished(onFinished)

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPage(page = pages[page])
        }

        // Skip button
        if (!isLastPage) {
            TextButton(
                onClick = ::finish,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 8.dp, top = 4.dp)
            ) {
                Text(
                    "Saltar",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DotsIndicator(
                total = pages.size,
                current = pagerState.currentPage
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isLastPage) finish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    contentColor = Color(0xFF0C0C1E)
                )
            ) {
                Text(
                    text = if (isLastPage) "Comenzar" else "Siguiente",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPage) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Background image — full screen
        Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay — oscurece la imagen y resalta el texto
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(page.overlayGradient))
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Spacer(Modifier.height(32.dp))

            // Opening quote mark
            Text(
                text = "\u201C",
                fontSize = 64.sp,
                color = AccentPurple.copy(alpha = 0.5f),
                fontFamily = FontFamily.Serif,
                lineHeight = 32.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(Modifier.height(4.dp))

            // Quote text
            Text(
                text = page.quote,
                fontSize = 24.sp,
                lineHeight = 38.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1.5f))
        }
    }
}

@Composable
private fun DotsIndicator(total: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            Dot(isSelected = index == current)
        }
    }
}

@Composable
private fun Dot(isSelected: Boolean) {
    val width: Dp by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 8.dp,
        animationSpec = tween(durationMillis = 300),
        label = "dotWidth"
    )
    Box(
        modifier = Modifier
            .height(8.dp)
            .width(width)
            .clip(CircleShape)
            .background(if (isSelected) AccentPurple else AccentPurpleDim.copy(alpha = 0.4f))
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Onboarding — página 1", showSystemUi = true)
@Composable
private fun PreviewOnboardingPage1() {
    QuoteAnimeTheme {
        OnboardingPage(page = pages[0])
    }
}

@Preview(name = "Onboarding — página 2", showSystemUi = true)
@Composable
private fun PreviewOnboardingPage2() {
    QuoteAnimeTheme {
        OnboardingPage(page = pages[1])
    }
}

@Preview(name = "Dots — primera seleccionada", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PreviewDotsFirst() {
    QuoteAnimeTheme {
        DotsIndicator(total = 3, current = 0)
    }
}

@Preview(name = "Dots — segunda seleccionada", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PreviewDotsMiddle() {
    QuoteAnimeTheme {
        DotsIndicator(total = 3, current = 1)
    }
}
