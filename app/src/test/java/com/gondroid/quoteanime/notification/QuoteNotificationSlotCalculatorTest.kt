package com.gondroid.quoteanime.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Scenarios covered:
 *  - Slots spread end to end across the window for 1, 2, 3, 5 and 10 per day
 *  - A window that crosses midnight
 *  - start == end is a full 24 h window, spread without a duplicated end point
 *  - Out-of-range frequencies are clamped to 1..10
 *  - nextSlot before, between, on and after the day's slots
 *  - nextSlot inside a window that started the previous day
 *  - isWithinWindow across midnight and with a grace period after the end
 */
class QuoteNotificationSlotCalculatorTest {

    private val eight = LocalTime.of(8, 0)
    private val twentyTwo = LocalTime.of(22, 0)
    private val two = LocalTime.of(2, 0)

    private fun t(value: String) = LocalTime.parse(value)
    private fun dt(value: String) = LocalDateTime.parse(value)

    // ── slotsFor ─────────────────────────────────────────────────────────────

    @Test
    fun `given one per day, when slots are calculated, then it fires at the window start`() {
        assertEquals(listOf(t("08:00")), QuoteNotificationSlotCalculator.slotsFor(eight, twentyTwo, 1))
    }

    @Test
    fun `given two per day, when slots are calculated, then they sit on both ends of the window`() {
        assertEquals(
            listOf(t("08:00"), t("22:00")),
            QuoteNotificationSlotCalculator.slotsFor(eight, twentyTwo, 2)
        )
    }

    @Test
    fun `given three per day, when slots are calculated, then they are spread end to end`() {
        assertEquals(
            listOf(t("08:00"), t("15:00"), t("22:00")),
            QuoteNotificationSlotCalculator.slotsFor(eight, twentyTwo, 3)
        )
    }

    @Test
    fun `given five per day, when slots are calculated, then they are three and a half hours apart`() {
        assertEquals(
            listOf(t("08:00"), t("11:30"), t("15:00"), t("18:30"), t("22:00")),
            QuoteNotificationSlotCalculator.slotsFor(eight, twentyTwo, 5)
        )
    }

    @Test
    fun `given ten per day, when slots are calculated, then there are ten and the last is exactly the window end`() {
        val slots = QuoteNotificationSlotCalculator.slotsFor(eight, twentyTwo, 10)

        assertEquals(10, slots.size)
        assertEquals(t("08:00"), slots.first())
        assertEquals(t("22:00"), slots.last())
    }

    @Test
    fun `given a window that crosses midnight, when slots are calculated, then they wrap past 00h`() {
        assertEquals(
            listOf(t("22:00"), t("00:00"), t("02:00")),
            QuoteNotificationSlotCalculator.slotsFor(twentyTwo, two, 3)
        )
    }

    @Test
    fun `given start equal to end, when slots are calculated, then the whole day is split without repeating the start`() {
        assertEquals(
            listOf(t("08:00"), t("14:00"), t("20:00"), t("02:00")),
            QuoteNotificationSlotCalculator.slotsFor(eight, eight, 4)
        )
    }

    @Test
    fun `given a frequency below one, when slots are calculated, then it is treated as one`() {
        assertEquals(listOf(t("08:00")), QuoteNotificationSlotCalculator.slotsFor(eight, twentyTwo, 0))
    }

    @Test
    fun `given a frequency above ten, when slots are calculated, then it is capped at ten`() {
        assertEquals(10, QuoteNotificationSlotCalculator.slotsFor(eight, twentyTwo, 15).size)
    }

    // ── nextSlot ─────────────────────────────────────────────────────────────

    @Test
    fun `given it is before the window, when the next slot is calculated, then it is today's first slot`() {
        assertEquals(
            dt("2026-07-25T08:00"),
            QuoteNotificationSlotCalculator.nextSlot(dt("2026-07-25T07:00"), eight, twentyTwo, 3)
        )
    }

    @Test
    fun `given it is between slots, when the next slot is calculated, then it is the following one today`() {
        assertEquals(
            dt("2026-07-25T15:00"),
            QuoteNotificationSlotCalculator.nextSlot(dt("2026-07-25T09:00"), eight, twentyTwo, 3)
        )
    }

    @Test
    fun `given it is exactly a slot, when the next slot is calculated, then that slot is skipped`() {
        assertEquals(
            dt("2026-07-25T22:00"),
            QuoteNotificationSlotCalculator.nextSlot(dt("2026-07-25T15:00"), eight, twentyTwo, 3)
        )
    }

    @Test
    fun `given the last slot passed, when the next slot is calculated, then it is tomorrow's first`() {
        assertEquals(
            dt("2026-07-26T08:00"),
            QuoteNotificationSlotCalculator.nextSlot(dt("2026-07-25T23:00"), eight, twentyTwo, 3)
        )
    }

    @Test
    fun `given a midnight window before 00h, when the next slot is calculated, then it is midnight`() {
        assertEquals(
            dt("2026-07-26T00:00"),
            QuoteNotificationSlotCalculator.nextSlot(dt("2026-07-25T23:00"), twentyTwo, two, 3)
        )
    }

    @Test
    fun `given a midnight window that started yesterday, when the next slot is calculated, then it is today's 2am`() {
        assertEquals(
            dt("2026-07-26T02:00"),
            QuoteNotificationSlotCalculator.nextSlot(dt("2026-07-26T01:00"), twentyTwo, two, 3)
        )
    }

    @Test
    fun `given a midnight window already closed, when the next slot is calculated, then it is tonight's opening`() {
        assertEquals(
            dt("2026-07-26T22:00"),
            QuoteNotificationSlotCalculator.nextSlot(dt("2026-07-26T03:00"), twentyTwo, two, 3)
        )
    }

    // ── isWithinWindow ───────────────────────────────────────────────────────

    @Test
    fun `given a midnight window, when it is 1am, then it is inside`() {
        assertTrue(QuoteNotificationSlotCalculator.isWithinWindow(t("01:00"), twentyTwo, two))
    }

    @Test
    fun `given a midnight window, when it is noon, then it is outside`() {
        assertFalse(QuoteNotificationSlotCalculator.isWithinWindow(t("12:00"), twentyTwo, two))
    }

    @Test
    fun `given a minute before the window, when checked, then it is outside`() {
        assertFalse(QuoteNotificationSlotCalculator.isWithinWindow(t("07:59"), eight, twentyTwo))
    }

    @Test
    fun `given a run twenty minutes after the end, when checked with a thirty minute grace, then it is inside`() {
        assertTrue(QuoteNotificationSlotCalculator.isWithinWindow(t("22:20"), eight, twentyTwo, graceMinutes = 30))
    }

    @Test
    fun `given a run forty minutes after the end, when checked with a thirty minute grace, then it is outside`() {
        assertFalse(QuoteNotificationSlotCalculator.isWithinWindow(t("22:40"), eight, twentyTwo, graceMinutes = 30))
    }
}
