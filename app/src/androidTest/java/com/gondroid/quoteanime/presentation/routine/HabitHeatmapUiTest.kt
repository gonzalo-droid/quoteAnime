package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - The grid renders
 *  - A tap on a past day inside the range reports a date
 *  - A tap on a day before startDate reports nothing
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

        composeRule.onNodeWithTag("habit_heatmap").performTouchInput { click(center) }

        assertTrue("expected a date from the center of the grid", clicked != null)
    }

    @Test
    fun tappingBeforeTheStartDateReportsNothing() {
        var clicked: LocalDate? = null
        // Habit starts today: every cell except today's is outside the range
        setContent(startDate = today) { clicked = it }

        composeRule.onNodeWithTag("habit_heatmap").performTouchInput { click(topLeft) }

        assertNull(clicked)
    }
}
