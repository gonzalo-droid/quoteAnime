package com.gondroid.quoteanime.presentation.routine

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The editor's color swatches expose their selected state to TalkBack through
 * [HabitPalette.isSelected]; this pins which swatch that is. (The semantics themselves are
 * checked by the instrumented `HabitSelectionSemanticsUiTest` — `app/src/test` has no
 * Robolectric to run Compose on the JVM.)
 *
 * Scenarios covered:
 *  - Exactly one swatch is selected, the one at the stored index
 *  - An out-of-range index selects the swatch [HabitPalette.colorAt] actually draws, so the
 *    announced color and the shown color never disagree
 */
class HabitPaletteTest {

    private val swatches = HabitPalette.COLORS.indices

    @Test
    fun `given a stored index, when checking every swatch, then only that one is selected`() {
        assertEquals(listOf(3), swatches.filter { HabitPalette.isSelected(it, selectedIndex = 3) })
    }

    @Test
    fun `given an out-of-range index, when checking every swatch, then the selected one is the color shown`() {
        listOf(HabitPalette.COLORS.size + 2, -1).forEach { stored ->
            val selected = swatches.filter { HabitPalette.isSelected(it, selectedIndex = stored) }

            assertEquals(1, selected.size)
            assertEquals(HabitPalette.colorAt(stored), HabitPalette.COLORS[selected.single()])
        }
    }
}
