package com.gondroid.quoteanime.domain.model

import java.util.Locale

/**
 * A quote from `/quotes` in the Realtime Database.
 *
 * Two fields classify it, and they must not be mixed up:
 *  - [anime] — the series it comes from ("Naruto"). The Settings **anime selection**
 *    (`UserPreferences.selectedCategoryIds`) filters on it: Home feed, notifications and widget.
 *    See `AnimeSelection.kt`.
 *  - [categories] — its **emotions** ("motivación", "reflexión"). Only the Catalogue's emotion
 *    filter reads them (`GetQuotesByCategoryUseCase`, `CatalogFilter.ByEmotion`).
 */
data class Quote(
    val id: String,
    val anime: String?,
    val author: String?,
    /** The quote in Spanish — every quote has it. */
    val quote: String?,
    /** English translation (`quoteEn`); null until the quote is translated. */
    val quoteEn: String? = null,
    /** Emotion ids, stored in Spanish in the database. Not anime names. */
    val categories: List<String> = emptyList(),
    val animeSlug: String? = null,   // identifier used to resolve images from /imagenes/{slug}
    val imageUrl: String? = null,    // resolved at runtime from the /imagenes node
    val isFavorite: Boolean = false
)

/** True when this quote is tagged with the emotion [emotionId] — the Catalogue's filter. */
fun Quote.hasEmotion(emotionId: String): Boolean = emotionId in categories

/**
 * The text to show for [locale]: [Quote.quote] in Spanish, [Quote.quoteEn] in any other
 * language (the app's UI falls back to English too). A missing or blank translation shows the
 * Spanish text instead, so a quote is never empty just because it isn't translated yet.
 */
fun Quote.textFor(locale: Locale): String {
    val spanish = quote?.takeIf { it.isNotBlank() }
    val english = quoteEn?.takeIf { it.isNotBlank() }
    val text = if (locale.language == "es") spanish ?: english else english ?: spanish
    return text.orEmpty()
}
