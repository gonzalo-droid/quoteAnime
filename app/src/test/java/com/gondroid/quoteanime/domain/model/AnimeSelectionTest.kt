package com.gondroid.quoteanime.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The anime selection rule shared by the Home feed, the notification and the widget pick, and
 * the emotion rule of the Catalogue — two separate classifications of a quote.
 *
 * Fixtures mirror production: every quote has emotion `categories` next to its `anime`.
 *
 * Scenarios covered:
 *  - Anime names: from `anime`, distinct, sorted like iOS, never the emotions; blanks dropped
 *  - isFromAnimes / filterByAnimes: match `anime`, ignore `categories`; empty = every quote
 *  - hasEmotion: matches `categories`, ignores `anime`
 *  - validAnimeSelection: all stale → empty (every anime); mix → only the valid ones;
 *    unknown list → untouched
 *  - pickRandomFromAnimes: selected animes only; stale selection → every anime; excludeId
 */
class AnimeSelectionTest {

    private val naruto = Quote(id = "1", anime = "Naruto", author = null, quote = null, categories = listOf("motivación", "reflexión"))
    private val onePiece = Quote(id = "2", anime = "One Piece", author = null, quote = null, categories = listOf("motivación"))
    private val bleach = Quote(id = "3", anime = "Bleach", author = null, quote = null, categories = listOf("amistad"))
    private val naruto2 = Quote(id = "4", anime = "Naruto", author = null, quote = null, categories = listOf("amistad"))
    private val noAnime = Quote(id = "5", anime = null, author = null, quote = null, categories = listOf("motivación"))
    private val all = listOf(onePiece, naruto, noAnime, bleach, naruto2)

    // ── Anime names ───────────────────────────────────────────────────────────

    @Test
    fun `given quotes tagged with emotions, then the anime names are their animes, not the emotions`() {
        assertEquals(listOf("Bleach", "Naruto", "One Piece"), all.animeNames())
    }

    @Test
    fun `given names with duplicates, blanks and mixed case, then they are distinct and sorted like iOS`() {
        // iOS: Array(Set(animes)).sorted() — plain, case-sensitive order (uppercase first).
        assertEquals(
            listOf("Attack on Titan", "Naruto", "one punch man"),
            animeNamesOf(listOf("Naruto", null, "one punch man", " ", "", "Attack on Titan", "Naruto"))
        )
    }

    // ── Anime filter ──────────────────────────────────────────────────────────

    @Test
    fun `given an empty selection, then every quote matches`() {
        all.forEach { assertTrue(it.isFromAnimes(emptySet())) }
        assertEquals(all, all.filterByAnimes(emptySet()))
    }

    @Test
    fun `given a selection, then the anime decides and the emotions are ignored`() {
        assertTrue(naruto.isFromAnimes(setOf("Naruto", "Bleach")))
        assertFalse(onePiece.isFromAnimes(setOf("Naruto", "Bleach")))
        assertFalse(noAnime.isFromAnimes(setOf("Naruto")))
        assertFalse(naruto.isFromAnimes(setOf("motivación")))
    }

    @Test
    fun `given a list, when filtered by anime, then the selected quotes keep their order`() {
        assertEquals(listOf(onePiece, naruto, naruto2), all.filterByAnimes(setOf("Naruto", "One Piece")))
    }

    // ── Emotion filter (Catalogue) ────────────────────────────────────────────

    @Test
    fun `given an emotion, then the categories decide and the anime is ignored`() {
        assertTrue(naruto.hasEmotion("motivación"))
        assertFalse(bleach.hasEmotion("motivación"))
        assertFalse(naruto.hasEmotion("Naruto"))
    }

    // ── Saved selection migration ─────────────────────────────────────────────

    @Test
    fun `given a saved selection of emotions only, then it becomes empty, meaning every anime`() {
        assertEquals(emptySet<String>(), validAnimeSelection(setOf("motivación", "reflexión"), all.animeNames()))
    }

    @Test
    fun `given a saved selection mixing animes and emotions, then only the animes are kept`() {
        assertEquals(setOf("Naruto"), validAnimeSelection(setOf("Naruto", "motivación"), all.animeNames()))
    }

    @Test
    fun `given a valid saved selection, then it is kept as is`() {
        assertEquals(setOf("Naruto", "Bleach"), validAnimeSelection(setOf("Naruto", "Bleach"), all.animeNames()))
    }

    @Test
    fun `given the anime list is unknown, then the saved selection is left untouched`() {
        assertEquals(setOf("motivación"), validAnimeSelection(setOf("motivación"), emptyList()))
    }

    // ── Random pick (notification and widget) ─────────────────────────────────

    @Test
    fun `given a selection, then the pick only comes from the selected animes`() {
        repeat(50) { seed ->
            assertEquals("Naruto", all.pickRandomFromAnimes(setOf("Naruto"), random = Random(seed))?.anime)
        }
    }

    @Test
    fun `given a stale emotion selection, then the pick comes from every anime`() {
        val picked = (0 until 200).mapNotNull {
            all.pickRandomFromAnimes(setOf("motivación"), random = Random(it))?.id
        }.toSet()
        assertEquals(all.map { it.id }.toSet(), picked)
    }

    @Test
    fun `given an excluded quote, then another one of the selection is picked`() {
        repeat(50) { seed ->
            assertEquals("4", all.pickRandomFromAnimes(setOf("Naruto"), excludeId = "1", random = Random(seed))?.id)
        }
    }

    @Test
    fun `given the excluded quote is the only one, then it is still picked`() {
        assertEquals("3", all.pickRandomFromAnimes(setOf("Bleach"), excludeId = "3")?.id)
    }

    @Test
    fun `given no quotes, then there is no pick`() {
        assertNull(emptyList<Quote>().pickRandomFromAnimes(setOf("Naruto")))
    }
}
