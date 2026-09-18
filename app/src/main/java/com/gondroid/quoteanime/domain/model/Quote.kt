package com.gondroid.quoteanime.domain.model

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
    val quote: String?,
    /** Emotion ids, stored in Spanish in the database. Not anime names. */
    val categories: List<String> = emptyList(),
    val animeSlug: String? = null,   // identifier used to resolve images from /imagenes/{slug}
    val imageUrl: String? = null,    // resolved at runtime from the /imagenes node
    val isFavorite: Boolean = false
)

/** True when this quote is tagged with the emotion [emotionId] — the Catalogue's filter. */
fun Quote.hasEmotion(emotionId: String): Boolean = emotionId in categories
