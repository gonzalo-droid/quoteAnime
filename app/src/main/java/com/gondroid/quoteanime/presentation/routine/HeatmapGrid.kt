package com.gondroid.quoteanime.presentation.routine

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Pure geometry of the contribution-style grid: columns are weeks, rows are
 * weekdays. Kept free of Compose so it can be unit tested.
 */
object HeatmapGrid {

    const val ROWS = 7

    /** Monday of the first visible week. */
    fun gridStart(today: LocalDate, weeks: Int): LocalDate =
        today.minusWeeks((weeks - 1).toLong())
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun dateAt(column: Int, row: Int, gridStart: LocalDate): LocalDate =
        gridStart.plusDays((column * ROWS + row).toLong())
}
