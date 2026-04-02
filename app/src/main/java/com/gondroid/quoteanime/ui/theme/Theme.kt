package com.gondroid.quoteanime.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary             = AccentPurple,
    onPrimary           = BgDark,
    primaryContainer    = AccentPurpleDim,
    onPrimaryContainer  = TextPrimary,

    secondary           = TextSecondary,
    onSecondary         = BgDark,

    background          = BgDark,
    onBackground        = TextPrimary,

    surface             = SurfaceDark,
    onSurface           = TextPrimary,
    surfaceVariant      = SurfaceVariant,
    onSurfaceVariant    = TextSecondary,

    outline             = OutlineColor,
    outlineVariant      = OutlineColor.copy(alpha = 0.5f),

    error               = HeartRed,
    onError             = Color.White,
)

@Composable
fun QuoteAnimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
