package com.gondroid.quoteanime.domain

import org.junit.Assert.assertEquals
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Test

/**
 * Guards the core library desugaring setup: the codebase relies on java.time
 * with minSdk 24, which only works with desugaring enabled.
 */
class JavaTimeAvailabilityTest {

    @Test
    fun `given an ISO string, when parsed, then it round-trips to the same text`() {
        val date = LocalDate.parse("2026-07-25")

        assertEquals("2026-07-25", date.toString())
        assertEquals(DayOfWeek.SATURDAY, date.dayOfWeek)
    }

    @Test
    fun `given a date, when subtracting one day, then the previous day is returned`() {
        val date = LocalDate.parse("2026-01-01")

        assertEquals(LocalDate.parse("2025-12-31"), date.minusDays(1))
    }
}
