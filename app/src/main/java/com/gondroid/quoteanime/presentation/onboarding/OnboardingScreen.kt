package com.gondroid.quoteanime.presentation.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.HabitTemplate
import com.gondroid.quoteanime.presentation.routine.HabitEditorError
import com.gondroid.quoteanime.presentation.routine.HabitPalette
import com.gondroid.quoteanime.presentation.routine.HabitThemeImages
import com.gondroid.quoteanime.presentation.routine.TemplateFilterChip
import com.gondroid.quoteanime.presentation.routine.ThemedSuggestionPreview
import com.gondroid.quoteanime.presentation.routine.resolveTemplateTitle
import com.gondroid.quoteanime.presentation.routine.resolveThemeDescription
import com.gondroid.quoteanime.ui.theme.AccentPurple
import com.gondroid.quoteanime.ui.theme.AccentPurpleDim
import com.gondroid.quoteanime.ui.theme.BgDark
import com.gondroid.quoteanime.ui.theme.SurfaceVariant
import com.gondroid.quoteanime.ui.theme.TextPrimary
import com.gondroid.quoteanime.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme

private data class OnboardingPage(
    @StringRes val quoteRes: Int,
    @DrawableRes val imageRes: Int,
    val overlayGradient: List<Color>
)

private val pages = listOf(
    OnboardingPage(
        quoteRes = R.string.onboarding_quote_1,
        imageRes = R.drawable.onboarding_01,
        overlayGradient = listOf(
            Color(0xFF0C0C1E).copy(alpha = 0.55f),
            Color(0xFF1A0E2E).copy(alpha = 0.75f),
            Color(0xFF0C0C1E).copy(alpha = 0.95f)
        )
    ),
    OnboardingPage(
        quoteRes = R.string.onboarding_quote_2,
        imageRes = R.drawable.onboarding_02,
        overlayGradient = listOf(
            Color(0xFF0A1020).copy(alpha = 0.55f),
            Color(0xFF0E1E3A).copy(alpha = 0.75f),
            Color(0xFF0A1020).copy(alpha = 0.95f)
        )
    ),
    OnboardingPage(
        quoteRes = R.string.onboarding_quote_3,
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
    onNavigateToPaywall: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pageCount = pages.size + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val isHabitPage = pagerState.currentPage == pages.size

    fun finish() = viewModel.onOnboardingFinished(onFinished)

    // Resolved here (composable scope) so both the shared bottom button and the habit page
    // itself read the same legible text — never the raw "template_xxx" resource key.
    val selectedTemplate = uiState.templates.find { it.id == uiState.selectedTemplateId }
    val resolvedSelectedTitle = selectedTemplate?.let { resolveTemplateTitle(it.title) }
    val canCreate = !isHabitPage || resolvedSelectedTitle != null

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page < pages.size) {
                OnboardingPage(page = pages[page])
            } else {
                HabitOnboardingPage(
                    templates = uiState.templates,
                    selectedTemplateId = uiState.selectedTemplateId,
                    isPremium = uiState.isPremium,
                    error = uiState.error,
                    onTemplateSelected = viewModel::onTemplateSelected,
                    onPremiumTemplateTapped = onNavigateToPaywall
                )
            }
        }

        // A single, always-visible skip control (instead of the previous split between a
        // top-right button on pages 1–3 and a separate one only on the habit page) so every
        // page in the flow offers the same way out.
        IconButton(
            onClick = ::finish,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 12.dp, top = 8.dp)
                .testTag("onboarding_skip")
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.onboarding_habit_skip),
                tint = TextSecondary
            )
        }

        // Dots + one primary button on every page: pages 1–3 advance the pager, the last
        // page creates the habit instead — same layout throughout, only the action changes.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DotsIndicator(
                total = pageCount,
                current = pagerState.currentPage
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isHabitPage) {
                        resolvedSelectedTitle?.let { title ->
                            viewModel.onCreateHabit(title, onFinished)
                        }
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                enabled = canCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_primary_action"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    contentColor = Color(0xFF0C0C1E)
                )
            ) {
                Text(
                    text = stringResource(
                        if (isHabitPage) R.string.onboarding_habit_create else R.string.onboarding_next
                    ),
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
                text = "“",
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
                text = stringResource(page.quoteRes),
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

/**
 * The habit-picker page. Purely presentational — the shared bottom action bar in
 * [OnboardingScreen] owns the Create/Skip buttons so this page's layout stays in sync with
 * pages 1–3 instead of reading as a bolted-on plain form at the end of three full-bleed
 * hero pages.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitOnboardingPage(
    templates: List<HabitTemplate>,
    selectedTemplateId: String?,
    isPremium: Boolean,
    error: HabitEditorError?,
    onTemplateSelected: (HabitTemplate) -> Unit,
    onPremiumTemplateTapped: () -> Unit
) {
    val selectedTemplate = templates.find { it.id == selectedTemplateId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BgDark, SurfaceVariant, BgDark))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(bottom = 140.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(AccentPurple.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = stringResource(R.string.onboarding_habit_title),
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                text = stringResource(R.string.onboarding_habit_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 24.dp)
            ) {
                templates.forEach { template ->
                    TemplateFilterChip(
                        template = template,
                        label = resolveTemplateTitle(template.title),
                        selected = selectedTemplateId == template.id,
                        isPremium = isPremium,
                        onSelected = { onTemplateSelected(template) },
                        onPremiumTapped = onPremiumTemplateTapped
                    )
                }
            }

            selectedTemplate?.let { template ->
                ThemedSuggestionPreview(
                    iconKey = template.iconKey,
                    title = resolveTemplateTitle(template.title),
                    description = resolveThemeDescription(template.themeKey).orEmpty(),
                    imageRes = HabitThemeImages.resFor(template.themeKey),
                    accentColor = HabitPalette.colorAt(template.themeColorIndex ?: 0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                )
            }

            error?.let {
                Text(
                    text = when (it) {
                        HabitEditorError.BlankTitle -> stringResource(R.string.habit_editor_error_blank)
                        HabitEditorError.InvalidDateRange -> stringResource(R.string.habit_editor_error_dates)
                        is HabitEditorError.LimitReached -> stringResource(R.string.habit_editor_error_limit, it.max)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
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

@Preview(name = "Onboarding — habit page", showSystemUi = true)
@Composable
private fun PreviewHabitOnboardingPage() {
    QuoteAnimeTheme {
        HabitOnboardingPage(
            templates = com.gondroid.quoteanime.domain.model.DefaultHabitTemplates.ALL,
            selectedTemplateId = "theme_ninja",
            isPremium = false,
            error = null,
            onTemplateSelected = {},
            onPremiumTemplateTapped = {}
        )
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
