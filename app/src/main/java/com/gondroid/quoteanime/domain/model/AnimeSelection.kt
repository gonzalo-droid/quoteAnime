package com.gondroid.quoteanime.domain.model

import kotlin.random.Random

/*
 * The Settings anime selection: which series the Home feed, the quote notifications and the
 * quote widget draw from. It is stored as `UserPreferences.selectedCategoryIds` (a name kept for
 * the DataStore key and for parity with iOS) and holds **anime names**, compared against
 * `Quote.anime` — never against `Quote.categories`, which are emotions.
 *
 * An empty selection means every anime.
 */

/**
 * The distinct anime names of [animes], sorted the way iOS sorts them
 * (`GetCategoriesUseCase`: `Array(Set(quotes.map { $0.anime })).sorted()`, a plain,
 * case-sensitive string order). Missing or blank names are dropped: they can't be chosen.
 */
fun animeNamesOf(animes: Iterable<String?>): List<String> =
    animes.filterNotNull().filter { it.isNotBlank() }.distinct().sorted()

/** The distinct anime names of these quotes — see [animeNamesOf]. */
fun List<Quote>.animeNames(): List<String> = animeNamesOf(map { it.anime })

/** True when this quote comes from one of [animes]; an empty selection matches every quote. */
fun Quote.isFromAnimes(animes: Set<String>): Boolean = animes.isEmpty() || anime in animes

/** The quotes of the anime selection, in their original order — see [isFromAnimes]. */
fun List<Quote>.filterByAnimes(animes: Set<String>): List<Quote> =
    if (animes.isEmpty()) this else filter { it.isFromAnimes(animes) }

/**
 * The part of a saved selection that still names an existing anime.
 *
 * A saved value that matches no anime (a series removed from the database, or an emotion saved
 * by the build that listed emotions under "Animes") is dropped. If none survive the result is
 * empty — "every anime" — so a stale selection never leaves the feed empty.
 *
 * While the anime list is unknown ([available] empty: not loaded yet, offline) the selection is
 * returned untouched, so a slow network can't wipe a valid choice.
 */
fun validAnimeSelection(saved: Set<String>, available: Collection<String>): Set<String> =
    if (saved.isEmpty() || available.isEmpty()) saved else saved.intersect(available.toSet())

/**
 * A random quote of the anime selection for the notification and the widget, avoiding
 * [excludeId] (the last one shown) when there is another choice. A stale selection is read as
 * "every anime", like everywhere else — see [validAnimeSelection].
 */
fun List<Quote>.pickRandomFromAnimes(
    animes: Set<String>,
    excludeId: String? = null,
    random: Random = Random.Default
): Quote? {
    val pool = filterByAnimes(validAnimeSelection(animes, animeNames()))
    val candidates = if (!excludeId.isNullOrEmpty() && pool.size > 1) pool.filter { it.id != excludeId } else pool
    return candidates.randomOrNull(random)
}
