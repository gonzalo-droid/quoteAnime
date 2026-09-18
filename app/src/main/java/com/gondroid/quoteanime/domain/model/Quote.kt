package com.gondroid.quoteanime.domain.model

data class Quote(
    val id: String,
    val anime: String?,
    val author: String?,
    val quote: String?,
    val categories: List<String> = emptyList(),
    val animeSlug: String? = null,   // identifier used to resolve images from /imagenes/{slug}
    val imageUrl: String? = null,    // resolved at runtime from the /imagenes node
    val isFavorite: Boolean = false
)

/**
 * True when this quote belongs to one of [categoryIds], the user's anime selection. **An empty
 * selection means every anime**, as everywhere else `selectedCategoryIds` is read.
 *
 * Same rule as the notification and widget pick (`QuoteRemoteDataSource.getRandomQuote`): a quote
 * with a `categories` list matches on it, an older one on its `anime` field.
 */
fun Quote.isInCategories(categoryIds: Set<String>): Boolean = when {
    categoryIds.isEmpty() -> true
    categories.isNotEmpty() -> categories.any { it in categoryIds }
    else -> anime in categoryIds
}

/** The quotes of the user's anime selection — see [isInCategories]. */
fun List<Quote>.filterByCategories(categoryIds: Set<String>): List<Quote> =
    if (categoryIds.isEmpty()) this else filter { it.isInCategories(categoryIds) }
