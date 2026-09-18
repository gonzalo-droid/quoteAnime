package com.gondroid.quoteanime.notification

import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure calculation of when quote notifications fire. Kept separate from WorkManager so the
 * window arithmetic — including windows that cross midnight — can be unit tested.
 *
 * Slots are spread end to end across the window: 3 per day in 08:00–22:00 fire at
 * 08:00, 15:00 and 22:00. A window whose start equals its end covers the whole day.
 */
object QuoteNotificationSlotCalculator {

    private const val MINUTES_PER_DAY = 24 * 60
    private const val MIN_PER_DAY = 1
    private const val MAX_PER_DAY = 10

    fun slotsFor(start: LocalTime, end: LocalTime, timesPerDay: Int): List<LocalTime> =
        slotOffsets(start, end, timesPerDay).map { start.plusMinutes(it.toLong()) }

    fun nextSlot(from: LocalDateTime, start: LocalTime, end: LocalTime, timesPerDay: Int): LocalDateTime {
        val offsets = slotOffsets(start, end, timesPerDay)
        // A window that crosses midnight may have opened yesterday, so its late slots
        // (00:00, 02:00…) belong to the previous day's window.
        return (-1L..1L)
            .flatMap { day ->
                val windowStart = from.toLocalDate().plusDays(day).atTime(start)
                offsets.map { windowStart.plusMinutes(it.toLong()) }
            }
            .filter { it.isAfter(from) }
            .min()
    }

    fun isWithinWindow(now: LocalTime, start: LocalTime, end: LocalTime, graceMinutes: Int = 0): Boolean {
        val length = windowLength(start, end)
        if (length == MINUTES_PER_DAY) return true
        val sinceStart = (minutesOfDay(now) - minutesOfDay(start)).mod(MINUTES_PER_DAY)
        return sinceStart <= length + graceMinutes
    }

    private fun slotOffsets(start: LocalTime, end: LocalTime, timesPerDay: Int): List<Int> {
        val count = timesPerDay.coerceIn(MIN_PER_DAY, MAX_PER_DAY)
        if (count == 1) return listOf(0)
        val length = windowLength(start, end)
        // A full-day window has no separate end: spreading to both ends would fire twice at start.
        val gaps = if (length == MINUTES_PER_DAY) count else count - 1
        return (0 until count).map { k -> k * length / gaps }
    }

    private fun windowLength(start: LocalTime, end: LocalTime): Int {
        val length = (minutesOfDay(end) - minutesOfDay(start)).mod(MINUTES_PER_DAY)
        return if (length == 0) MINUTES_PER_DAY else length
    }

    private fun minutesOfDay(time: LocalTime) = time.hour * 60 + time.minute
}
