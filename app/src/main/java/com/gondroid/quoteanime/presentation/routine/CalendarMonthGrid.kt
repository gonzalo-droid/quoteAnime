package com.gondroid.quoteanime.presentation.routine

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Pure geometry of a classic month calendar: always 6 full Monday-Sunday weeks (42 days)
 * so every month renders the same grid height — switching months never reflows the rest
 * of the screen. Kept free of Compose so it can be unit tested.
 */
object CalendarMonthGrid {

    const val ROWS = 6
    const val COLUMNS = 7

    /** The 42 dates to display, including the leading/trailing days of adjacent months
     *  needed to fill out the first and last weeks. */
    fun daysFor(month: YearMonth): List<LocalDate> {
        val gridStart = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0 until ROWS * COLUMNS).map { gridStart.plusDays(it.toLong()) }
    }
}
