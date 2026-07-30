package com.gondroid.quoteanime.presentation.routine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Scenarios covered:
 *  - The grid always has 42 days (6 full Monday-Sunday weeks)
 *  - The grid starts on a Monday and ends on a Sunday
 *  - Every day of the requested month is included
 *  - Leading/trailing days from adjacent months fill out the first and last weeks
 */
class CalendarMonthGridTest {

    @Test
    fun `given any month, when built, then it has exactly 42 days`() {
        val days = CalendarMonthGrid.daysFor(YearMonth.of(2026, 7))

        assertEquals(42, days.size)
    }

    @Test
    fun `given any month, when built, then it starts on a Monday and ends on a Sunday`() {
        val days = CalendarMonthGrid.daysFor(YearMonth.of(2026, 7))

        assertEquals(DayOfWeek.MONDAY, days.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, days.last().dayOfWeek)
    }

    @Test
    fun `given July 2026, when built, then every day of July is present`() {
        val month = YearMonth.of(2026, 7)
        val days = CalendarMonthGrid.daysFor(month)

        val julyDays = days.filter { it.month == month.month && it.year == month.year }
        assertEquals(month.lengthOfMonth(), julyDays.size)
        assertTrue(julyDays.contains(LocalDate.parse("2026-07-01")))
        assertTrue(julyDays.contains(LocalDate.parse("2026-07-31")))
    }

    @Test
    fun `given a month that already starts on a Monday, when built, then no leading days are needed`() {
        // June 2026 starts on a Monday, so the grid should begin exactly on June 1st.
        val month = YearMonth.of(2026, 6)
        val days = CalendarMonthGrid.daysFor(month)

        assertEquals(LocalDate.parse("2026-06-01"), days.first())
    }
}
