package com.gondroid.quoteanime.presentation.onboarding

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gondroid.quoteanime.ui.theme.AccentPurple
import com.gondroid.quoteanime.ui.theme.AccentPurpleDim
import com.gondroid.quoteanime.ui.theme.TextPrimary
import com.gondroid.quoteanime.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val number: String,
    val quote: String,
    val gradient: List<Color>
)

private val pages = listOf(
    OnboardingPage(
        number = "01",
        quote = "sabemos que no son solo historias, son hechos que marcaron nuestra vida",
        gradient = listOf(Color(0xFF0C0C1E), Color(0xFF1A0E2E), Color(0xFF0C0C1E))
    ),
    OnboardingPage(
        number = "02",
        quote = "hay una historia que nos lleva a cada momento de nuestra vida",
        gradient = listOf(Color(0xFF0A1020), Color(0xFF0E1E3A), Color(0xFF0A1020))
    ),
    OnboardingPage(
        number = "03",
        quote = "los protagonistas crecieron con nosotros y aprendimos a nunca rendirnos",
        gradient = listOf(Color(0xFF12080E), Color(0xFF260A20), Color(0xFF12080E))
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

        // Bottom bar: dots + button
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
                    if (isLastPage) {
                        finish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(page.gradient))
    ) {
        // Large decorative number in background
        Text(
            text = page.number,
            fontSize = 220.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = AccentPurple.copy(alpha = 0.05f),
            modifier = Modifier.align(Alignment.Center)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            // Page number label
            Text(
                text = page.number,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AccentPurple.copy(alpha = 0.7f),
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(32.dp))

            // Opening quote mark
            Text(
                text = "\u201C",
                fontSize = 64.sp,
                color = AccentPurple.copy(alpha = 0.3f),
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
