package com.gondroid.quoteanime.notification

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure calculation of the next reminder instant. Kept separate from WorkManager
 * so the weekday logic can be unit tested.
 */
object NextReminderCalculator {

    fun nextOccurrence(
        from: LocalDateTime,
        time: LocalTime,
        days: Set<DayOfWeek>
    ): LocalDateTime? {
        if (days.isEmpty()) return null

        for (offset in 0..7) {
            val candidate = from.toLocalDate().plusDays(offset.toLong()).atTime(time)
            if (candidate.dayOfWeek in days && candidate.isAfter(from)) return candidate
        }
        return null
    }
}
