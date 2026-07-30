package com.gondroid.quoteanime.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.gondroid.quoteanime.R

val Georgia: FontFamily = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.lora_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.lora_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

val Didot: FontFamily = FontFamily(
    Font(R.font.playfair_display_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.playfair_display_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.playfair_display_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.playfair_display_bold, FontWeight.Bold, FontStyle.Normal),
)

// ── Candidates evaluated for the Home quote text (see FONT_OPTIONS.md) ────────

// Fraunces — soft, characterful old-style serif with a slightly "wonky" warmth.
// Reads as literary and a little playful at once, which fits the app's
// dark/anime/poetic register without feeling as stiff-formal as Didot/Georgia.
// Chosen as the new default for the Home quote text.
val Fraunces: FontFamily = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.fraunces_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.fraunces_bold, FontWeight.Bold, FontStyle.Normal),
)

// Cormorant Garamond — delicate, high-x-height-contrast old-style serif.
// More refined/quiet than Georgia (Lora); good for a gentler, more intimate feel.
val CormorantGaramond: FontFamily = FontFamily(
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.cormorant_garamond_bold, FontWeight.Bold, FontStyle.Normal),
)

// Bodoni Moda — high-contrast modern/didone serif with a dramatic, editorial
// "manga volume title" energy. Bolder statement than the current pair.
val BodoniModa: FontFamily = FontFamily(
    Font(R.font.bodoni_moda_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.bodoni_moda_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.bodoni_moda_bold, FontWeight.Bold, FontStyle.Normal),
)

// Abril Fatface — single-weight display serif, very bold and dramatic.
// Only ships a Regular (display faces like this rarely need more weights);
// best used sparingly (e.g. a short pull-quote or hero word), not for long text.
val AbrilFatface: FontFamily = FontFamily(
    Font(R.font.abril_fatface_regular, FontWeight.Normal, FontStyle.Normal),
)
