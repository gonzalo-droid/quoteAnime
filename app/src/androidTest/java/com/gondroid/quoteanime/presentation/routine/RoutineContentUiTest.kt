package com.gondroid.quoteanime.presentation.routine

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.model.StreakState
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - Empty state offers the create action
 *  - Habit cards render with the header
 *  - Marking today reports the habit id
 *  - The add button disappears once the limit is reached
 */
@RunWith(AndroidJUnit4::class)
class RoutineContentUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.parse("2026-07-25")

    private fun progress(id: String) = HabitWithProgress(
        habit = Habit(
            id = id,
            title = "Entrenar $id",
            iconKey = "dumbbell",
            colorIndex = 0,
            startDate = today.minusMonths(1)
        ),
        completions = setOf(today),
        streak = StreakState(1, 3, today, true),
        completionRate = 0.4f
    )

    private fun setContent(
        state: RoutineUiState,
        onToggleToday: (String) -> Unit = {},
        onAddHabit: () -> Unit = {}
    ) {
        composeRule.setContent {
            QuoteAnimeTheme {
                RoutineContent(
                    state = state,
                    today = today,
                    onNavigateBack = {},
                    onToggleToday = onToggleToday,
                    onToggleDay = { _, _ -> },
                    onArchiveHabit = {},
                    onUnarchiveHabit = {},
                    onFilterChanged = {},
                    onAddHabit = onAddHabit,
                    onEditHabit = {},
                    onOpenHabitDetail = {},
                    onMessageShown = {},
                    onIntroDismissed = {},
                    onNavigateToPaywall = {}
                )
            }
        }
    }

    @Test
    fun emptyStateOffersTheCreateAction() {
        var clicked = false
        setContent(RoutineUiState(isLoading = false, maxHabits = 3), onAddHabit = { clicked = true })

        composeRule.onNodeWithTag("empty_add_habit").assertIsDisplayed().performClick()

        assertTrue(clicked)
    }

    @Test
    fun habitCardsAreDisplayed() {
        setContent(
            RoutineUiState(
                habits = listOf(progress("h1")),
                globalStreak = StreakState(5, 9, today, true),
                isLoading = false,
                maxHabits = 3
            )
        )

        // The streak header this test also asserted was dropped from the screen in
        // "feat: update habit tracker widget"; the tag has not existed since.
        composeRule.onNodeWithTag("habit_card_h1").assertIsDisplayed()
    }

    @Test
    fun markingTodayReportsTheHabitId() {
        var toggled: String? = null
        setContent(
            RoutineUiState(habits = listOf(progress("h1")), isLoading = false, maxHabits = 3),
            onToggleToday = { toggled = it }
        )

        composeRule.onNodeWithTag("toggle_today_h1").performClick()

        assertEquals("h1", toggled)
    }

    @Test
    fun addButtonDisappearsWhenTheLimitIsReached() {
        setContent(
            RoutineUiState(
                habits = listOf(progress("h1"), progress("h2"), progress("h3")),
                activeCount = 3,
                isLoading = false,
                maxHabits = 3
            )
        )

        // The button is not composed at all when the limit is reached, so the node must not exist
        composeRule.onNodeWithTag("add_habit_button").assertDoesNotExist()
    }
}
