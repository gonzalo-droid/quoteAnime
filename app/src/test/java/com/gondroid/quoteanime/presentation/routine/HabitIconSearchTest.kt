package com.gondroid.quoteanime.presentation.routine

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Scenarios covered:
 *  - An unaccented query finds an accented name ("musica" → "Tocar música"), and the reverse
 *  - Case is ignored
 *  - Surrounding whitespace in the query is ignored
 *  - A blank query (empty or only spaces) returns every category untouched
 *  - A query that matches nothing returns no categories
 *  - A category keeps only its matching keys, and one left with none disappears
 *  - The key itself is not searched, only the visible name
 */
class HabitIconSearchTest {

    private val creativity = HabitIconCategory(titleRes = 1, keys = listOf("music_note", "palette", "piano"))
    private val routine = HabitIconCategory(titleRes = 2, keys = listOf("bed", "local_cafe"))
    private val categories = listOf(creativity, routine)

    private val labels = mapOf(
        "music_note" to "Tocar música",
        "palette" to "Pintar",
        "piano" to "Practicar piano",
        "bed" to "Tender la cama",
        "local_cafe" to "Tomar café"
    )

    private fun search(query: String) = HabitIconSearch.filter(categories, query) { labels.getValue(it) }

    @Test
    fun `given a query without accents, when searching, then it finds the accented name`() {
        assertEquals(listOf(creativity.copy(keys = listOf("music_note"))), search("musica"))
    }

    @Test
    fun `given a query with accents, when searching, then it also finds the name`() {
        assertEquals(listOf(routine.copy(keys = listOf("local_cafe"))), search("CAFÉ"))
        assertEquals(listOf(routine.copy(keys = listOf("local_cafe"))), search("cafe"))
    }

    @Test
    fun `given a query in another case, when searching, then case is ignored`() {
        assertEquals(listOf(creativity.copy(keys = listOf("music_note"))), search("TOCAR MÚSICA"))
    }

    @Test
    fun `given a query with surrounding spaces, when searching, then they are trimmed`() {
        assertEquals(listOf(creativity.copy(keys = listOf("music_note"))), search("  musica "))
    }

    @Test
    fun `given an empty or blank query, when searching, then every category is returned`() {
        assertEquals(categories, search(""))
        assertEquals(categories, search("   "))
    }

    @Test
    fun `given a query that matches nothing, when searching, then no category is returned`() {
        assertEquals(emptyList<HabitIconCategory>(), search("xyz"))
    }

    @Test
    fun `given a query matching several icons, when searching, then each category keeps only its matches`() {
        // "Tocar", "Pintar", "Practicar" and "Tomar" match; "Tender la cama" doesn't.
        assertEquals(listOf(creativity, routine.copy(keys = listOf("local_cafe"))), search("ar"))
    }

    @Test
    fun `given a query matching only one category, when searching, then the other disappears`() {
        assertEquals(listOf(routine.copy(keys = listOf("bed"))), search("cama"))
    }

    @Test
    fun `given a query that only matches the key, when searching, then nothing is returned`() {
        assertEquals(emptyList<HabitIconCategory>(), search("music_note"))
    }
}
