package com.gondroid.quoteanime.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** [textFor]: `quote` in Spanish, `quoteEn` otherwise, Spanish when there is no translation. */
class QuoteTextTest {

    private val naruto = Quote(
        id = "1",
        anime = "Naruto",
        author = "Naruto Uzumaki",
        quote = "Nunca retrocedo a mi palabra, ese es mi camino ninja.",
        quoteEn = "I never go back on my word. That's my ninja way."
    )

    @Test
    fun `Spanish shows quote`() {
        assertEquals(naruto.quote, naruto.textFor(Locale.forLanguageTag("es")))
    }

    @Test
    fun `any Spanish region shows quote`() {
        assertEquals(naruto.quote, naruto.textFor(Locale.forLanguageTag("es-PE")))
    }

    @Test
    fun `English shows quoteEn`() {
        assertEquals(naruto.quoteEn, naruto.textFor(Locale.ENGLISH))
    }

    @Test
    fun `a language without its own texts shows quoteEn, like the rest of the app`() {
        assertEquals(naruto.quoteEn, naruto.textFor(Locale.FRENCH))
    }

    @Test
    fun `English without translation falls back to the Spanish text`() {
        val untranslated = naruto.copy(quoteEn = null)
        assertEquals(naruto.quote, untranslated.textFor(Locale.ENGLISH))
    }

    @Test
    fun `a blank translation counts as none`() {
        val blank = naruto.copy(quoteEn = "  ")
        assertEquals(naruto.quote, blank.textFor(Locale.ENGLISH))
    }

    @Test
    fun `Spanish without Spanish text shows the English one rather than nothing`() {
        val onlyEnglish = naruto.copy(quote = null)
        assertEquals(naruto.quoteEn, onlyEnglish.textFor(Locale.forLanguageTag("es")))
    }

    @Test
    fun `no text at all is empty`() {
        val empty = naruto.copy(quote = null, quoteEn = null)
        assertEquals("", empty.textFor(Locale.ENGLISH))
    }
}
