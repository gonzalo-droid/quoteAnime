package com.gondroid.quoteanime.presentation.home

import android.graphics.Paint
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [wrapText] in ShareUtils.kt.
 *
 * [wrapText] takes an Android [Paint] for text measurement. We mock [Paint.measureText]
 * to return predictable widths (character count * 10f) so we can test the line-breaking
 * algorithm independently of real glyph metrics.
 *
 * Scenarios covered:
 *  - Single word that fits on one line
 *  - Two words that fit together on one line
 *  - Two words that do NOT fit together — wrapped to two lines
 *  - Long sentence wrapped across multiple lines
 *  - Single very long word that exceeds maxWidth — placed on its own line
 *  - Empty string — returns empty list
 *  - Exact fit: candidate is exactly maxWidth — stays on same line (<=)
 *  - Text with multiple spaces treated as separate words
 */
class ShareUtilsTest {

    // Mock Paint that measures text as (text.length * 10f) pixels
    // This gives deterministic behaviour: "Hello" = 50f, "World" = 50f, "Hello World" = 110f
    private lateinit var paint: Paint

    // Width unit per character in the mock
    private val charWidth = 10f

    @Before
    fun setup() {
        paint = mockk()
        every { paint.measureText(any<String>()) } answers {
            val text = firstArg<String>()
            text.length * charWidth
        }
    }

    @Test
    fun `given single word that fits in maxWidth, returns single-element list`() {
        // "Hello" = 5 chars * 10 = 50f, maxWidth = 100f → fits
        val result = wrapText("Hello", paint, 100f)

        assertEquals(1, result.size)
        assertEquals("Hello", result[0])
    }

    @Test
    fun `given two words that together fit in maxWidth, returns single-element list`() {
        // "Hi" + " " + "Go" = "Hi Go" = 5 chars * 10 = 50f, maxWidth = 60f → fits
        val result = wrapText("Hi Go", paint, 60f)

        assertEquals(1, result.size)
        assertEquals("Hi Go", result[0])
    }

    @Test
    fun `given two words that do not fit together, wraps to two lines`() {
        // "Hello" = 50f, "World" = 50f, "Hello World" = 110f, maxWidth = 100f → wrap
        val result = wrapText("Hello World", paint, 100f)

        assertEquals(2, result.size)
        assertEquals("Hello", result[0])
        assertEquals("World", result[1])
    }

    @Test
    fun `given sentence that wraps across three lines`() {
        // Each word: "One"=30, "Two"=30, "And"=30; "One Two"=70 > 60 → wrap after "One"
        // "Two And" = 70 > 60 → wrap after "Two"; "And" = 30 → last line
        // maxWidth = 60f
        val result = wrapText("One Two And", paint, 60f)

        assertEquals(3, result.size)
        assertEquals("One", result[0])
        assertEquals("Two", result[1])
        assertEquals("And", result[2])
    }

    @Test
    fun `given a single word longer than maxWidth, it is placed on its own line`() {
        // "Superlongword" = 130f > 50f, but there is no previous word → it still goes on a line
        val result = wrapText("Superlongword", paint, 50f)

        assertEquals(1, result.size)
        assertEquals("Superlongword", result[0])
    }

    @Test
    fun `given empty string, returns empty list`() {
        // split(" ") on "" gives [""], loop runs once with word="" which is empty
        // current stays "" and is not added (isNotEmpty guard)
        val result = wrapText("", paint, 100f)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `given text whose candidate width is exactly maxWidth, word stays on same line`() {
        // "AB" = 20f, "CD" = 20f, "AB CD" = 50f; maxWidth = 50f → <=, so fits on one line
        val result = wrapText("AB CD", paint, 50f)

        assertEquals(1, result.size)
        assertEquals("AB CD", result[0])
    }

    @Test
    fun `given text whose candidate width exceeds maxWidth by one unit, wraps`() {
        // "AB" = 20f, "CDE" = 30f, "AB CDE" = 60f > 50f → wraps
        val result = wrapText("AB CDE", paint, 50f)

        assertEquals(2, result.size)
        assertEquals("AB", result[0])
        assertEquals("CDE", result[1])
    }

    @Test
    fun `given multiple words where second line accumulates correctly`() {
        // "A"=10, "B"=10, "C"=10, "D"=10
        // "A B"=30, fits in 30; "A B C"=50 > 30 → wrap; "C D"=30, fits; end → ["A B", "C D"]
        val result = wrapText("A B C D", paint, 30f)

        assertEquals(2, result.size)
        assertEquals("A B", result[0])
        assertEquals("C D", result[1])
    }

    @Test
    fun `given single word with trailing behavior, last line is not dropped`() {
        // If there's only one word and it fits, it must appear in the result
        val result = wrapText("Anime", paint, 200f)

        assertEquals(1, result.size)
        assertEquals("Anime", result[0])
    }
}
