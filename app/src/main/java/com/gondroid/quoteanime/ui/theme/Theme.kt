package com.gondroid.quoteanime.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// darkColorScheme() fills in every role we don't set here from Material's own stock
// dark theme — not from this palette. Left partially unset, that mismatch is exactly
// what made things like Switch's unchecked track and ModalBottomSheet's background
// look "off-brand": they read a role (surfaceContainerHighest / surfaceContainerLow)
// this app never defined, so it fell back to Material's default purple-gray instead
// of BgDark/SurfaceVariant. Every role below that a component in this app actually
// reads is set explicitly; the unused "Fixed" family (large-FAB/carousel roles) is
// left at Material's default since nothing here renders it.
private val AppColorScheme = darkColorScheme(
    primary             = AccentPurple,
    onPrimary           = BgDark,
    primaryContainer    = AccentPurpleDim,
    onPrimaryContainer  = TextPrimary,
    inversePrimary      = AccentPurpleDim,

    secondary           = TextSecondary,
    onSecondary         = BgDark,
    secondaryContainer  = SurfaceVariant,
    onSecondaryContainer = TextPrimary,

    tertiary            = HeartRed,
    onTertiary          = Color.White,
    tertiaryContainer   = RoseContainerDark,
    onTertiaryContainer = TextPrimary,

    background          = BgDark,
    onBackground        = TextPrimary,

    surface             = SurfaceDark,
    onSurface           = TextPrimary,
    surfaceVariant      = SurfaceVariant,
    onSurfaceVariant    = TextSecondary,
    surfaceTint         = AccentPurple,

    surfaceDim              = BgDark,
    surfaceBright           = SurfaceBright,
    surfaceContainerLowest  = BgDark,
    surfaceContainerLow     = SurfaceDark,
    surfaceContainer        = SurfaceVariant,
    surfaceContainerHigh    = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,

    inverseSurface      = TextPrimary,
    inverseOnSurface    = BgDark,

    outline             = OutlineColor,
    outlineVariant      = OutlineColor.copy(alpha = 0.5f),

    error               = HeartRed,
    onError             = Color.White,
    errorContainer      = RoseContainerDark,
    onErrorContainer    = TextPrimary,

    scrim               = Color.Black,
)

@Composable
fun QuoteAnimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
