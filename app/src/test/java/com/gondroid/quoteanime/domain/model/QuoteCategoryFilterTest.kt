package com.gondroid.quoteanime.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The anime-selection rule shared by the Home feed (and, in the data layer, by the notification
 * and widget pick).
 *
 * Scenarios covered:
 *  - Empty selection matches every quote, including one with no anime
 *  - A quote without `categories` matches on its `anime`
 *  - A quote with `categories` matches on them and ignores `anime`
 *  - filterByCategories keeps order and returns everything for an empty selection
 */
class QuoteCategoryFilterTest {

    private val naruto = Quote(id = "1", anime = "Naruto", author = null, quote = null)
    private val onePiece = Quote(id = "2", anime = "One Piece", author = null, quote = null)
    private val noAnime = Quote(id = "3", anime = null, author = null, quote = null)
    private val tagged = Quote(
        id = "4", anime = "Naruto", author = null, quote = null, categories = listOf("Shonen", "Ninjas")
    )

    @Test
    fun `given an empty selection, then every quote matches`() {
        listOf(naruto, onePiece, noAnime, tagged).forEach { assertTrue(it.isInCategories(emptySet())) }
    }

    @Test
    fun `given a quote without categories, then its anime decides`() {
        assertTrue(naruto.isInCategories(setOf("Naruto", "Bleach")))
        assertFalse(onePiece.isInCategories(setOf("Naruto", "Bleach")))
        assertFalse(noAnime.isInCategories(setOf("Naruto")))
    }

    @Test
    fun `given a quote with categories, then they decide and its anime is ignored`() {
        assertTrue(tagged.isInCategories(setOf("Ninjas")))
        assertFalse(tagged.isInCategories(setOf("Naruto")))
    }

    @Test
    fun `given a list, when filtered, then the selected quotes keep their order`() {
        val all = listOf(onePiece, naruto, noAnime, tagged)
        assertEquals(all, all.filterByCategories(emptySet()))
        assertEquals(listOf(onePiece, naruto), all.filterByCategories(setOf("Naruto", "One Piece")))
    }
}
