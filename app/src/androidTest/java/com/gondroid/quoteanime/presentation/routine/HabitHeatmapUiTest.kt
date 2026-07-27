package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - The grid renders
 *  - A tap on a past day inside the range reports a date
 *  - A tap on a day before startDate reports nothing (the cell doesn't even exist)
 *
 * Tap handling moved from raw pixel-offset gestures to a semantics-rich, per-cell
 * accessible overlay (see HabitHeatmap.kt), so these scenarios now interact with
 * individual cell nodes tagged "habit_heatmap_cell_<date>" instead of computing
 * pixel offsets on the "habit_heatmap"-tagged node.
 */
@RunWith(AndroidJUnit4::class)
class HabitHeatmapUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.parse("2026-07-25")

    private fun setContent(startDate: LocalDate, onDayClick: (LocalDate) -> Unit) {
        composeRule.setContent {
            QuoteAnimeTheme {
                HabitHeatmap(
                    completions = emptySet(),
                    colorIndex = 0,
                    today = today,
                    startDate = startDate,
                    endDate = null,
                    onDayClick = onDayClick,
                    modifier = Modifier.width(340.dp)
                )
            }
        }
    }

    @Test
    fun heatmapIsDisplayed() {
        setContent(startDate = today.minusMonths(2)) { }

        composeRule.onNodeWithTag("habit_heatmap").assertIsDisplayed()
    }

    @Test
    fun tappingAPastDayInsideTheRangeReportsADate() {
        var clicked: LocalDate? = null
        setContent(startDate = today.minusMonths(3)) { clicked = it }

        // "Today" is always in range and never in the future, so its accessible
        // cell is guaranteed to exist regardless of the grid's pixel layout.
        composeRule.onNodeWithTag("habit_heatmap_cell_$today").performClick()

        assertEquals(today, clicked)
    }

    @Test
    fun tappingBeforeTheStartDateReportsNothing() {
        var clicked: LocalDate? = null
        // Habit starts today: yesterday is outside the range, so it must have no
        // accessible cell at all -- not merely a disabled one.
        val yesterday = today.minusDays(1)
        setContent(startDate = today) { clicked = it }

        composeRule.onNodeWithTag("habit_heatmap_cell_$yesterday").assertDoesNotExist()

        assertNull(clicked)
    }
}
