package com.gondroid.quoteanime.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Scenarios covered:
 *  - Today is a reminder day and the time has not passed yet
 *  - Today is a reminder day but the time already passed
 *  - The next reminder day is later this week
 *  - The next reminder day wraps into next week
 *  - Every day selected
 *  - No days selected
 */
class NextReminderCalculatorTest {

    // Saturday 2026-07-25 at 10:00
    private val saturdayMorning = LocalDateTime.parse("2026-07-25T10:00")

    @Test
    fun `given today is selected and the time has not passed, when calculated, then it is today`() {
        val next = NextReminderCalculator.nextOccurrence(
            from = saturdayMorning,
            time = LocalTime.of(18, 0),
            days = setOf(DayOfWeek.SATURDAY)
        )

        assertEquals(LocalDateTime.parse("2026-07-25T18:00"), next)
    }

    @Test
    fun `given today is selected but the time passed, when calculated, then it is next week`() {
        val next = NextReminderCalculator.nextOccurrence(
            from = saturdayMorning,
            time = LocalTime.of(7, 0),
            days = setOf(DayOfWeek.SATURDAY)
        )

        assertEquals(LocalDateTime.parse("2026-08-01T07:00"), next)
    }

    @Test
    fun `given a later day this week, when calculated, then that day is returned`() {
        val next = NextReminderCalculator.nextOccurrence(
            from = saturdayMorning,
            time = LocalTime.of(7, 0),
            days = setOf(DayOfWeek.SUNDAY)
        )

        assertEquals(LocalDateTime.parse("2026-07-26T07:00"), next)
    }

    @Test
    fun `given only earlier weekdays, when calculated, then it wraps into next week`() {
        val next = NextReminderCalculator.nextOccurrence(
            from = saturdayMorning,
            time = LocalTime.of(7, 0),
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        )

        assertEquals(LocalDateTime.parse("2026-07-27T07:00"), next)
    }

    @Test
    fun `given every day selected and the time passed, when calculated, then it is tomorrow`() {
        val next = NextReminderCalculator.nextOccurrence(
            from = saturdayMorning,
            time = LocalTime.of(7, 0),
            days = DayOfWeek.entries.toSet()
        )

        assertEquals(LocalDateTime.parse("2026-07-26T07:00"), next)
    }

    @Test
    fun `given no days selected, when calculated, then there is no next occurrence`() {
        assertNull(
            NextReminderCalculator.nextOccurrence(
                from = saturdayMorning,
                time = LocalTime.of(7, 0),
                days = emptySet()
            )
        )
    }
}
