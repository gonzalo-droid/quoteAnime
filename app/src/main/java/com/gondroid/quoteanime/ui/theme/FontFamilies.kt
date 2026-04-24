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
