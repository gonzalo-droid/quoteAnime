package com.gondroid.quoteanime.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.gondroid.quoteanime.R
import androidx.compose.ui.text.googlefonts.Font as GFont

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Lora: estilo transitional-serif idéntico a Georgia, optimizada para pantalla.
private val loraFont = GoogleFont("Lora")

// Playfair Display: fuente Didone de referencia en Google Fonts, visualmente igual a Didot.
private val playfairFont = GoogleFont("Playfair Display")

// Georgia — descarga Lora desde Google Fonts; mientras carga (o si falla) usa la Georgia
// del sistema cuando está disponible (Samsung, muchos OEMs), y como último recurso el serif
// por defecto del dispositivo.
val Georgia: FontFamily = FontFamily(
    GFont(
        googleFont = loraFont,
        fontProvider = provider,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ),
    GFont(
        googleFont = loraFont,
        fontProvider = provider,
        weight = FontWeight.Normal,
        style = FontStyle.Italic
    ),
    GFont(
        googleFont = loraFont,
        fontProvider = provider,
        weight = FontWeight.Bold,
        style = FontStyle.Normal
    ),
    GFont(
        googleFont = loraFont,
        fontProvider = provider,
        weight = FontWeight.Bold,
        style = FontStyle.Italic
    )
)

// Didot — descarga Playfair Display desde Google Fonts; fallback al serif del sistema.
val Didot: FontFamily = FontFamily(
    GFont(
        googleFont = playfairFont,
        fontProvider = provider,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ),
    GFont(
        googleFont = playfairFont,
        fontProvider = provider,
        weight = FontWeight.Normal,
        style = FontStyle.Italic
    ),
    GFont(
        googleFont = playfairFont,
        fontProvider = provider,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal
    ),
    GFont(
        googleFont = playfairFont,
        fontProvider = provider,
        weight = FontWeight.Bold,
        style = FontStyle.Normal
    )
)
