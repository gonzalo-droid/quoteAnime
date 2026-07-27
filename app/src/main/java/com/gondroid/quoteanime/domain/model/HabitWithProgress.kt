package com.gondroid.quoteanime.domain.model

import java.time.LocalDate

/**
 * What the routine screen needs to draw one habit card: the habit itself, the
 * completions inside the visible heatmap window, its streak, and how much of its
 * active window has been completed.
 */
data class HabitWithProgress(
    val habit: Habit,
    val completions: Set<LocalDate>,
    val streak: StreakState,
    val completionRate: Float
)
