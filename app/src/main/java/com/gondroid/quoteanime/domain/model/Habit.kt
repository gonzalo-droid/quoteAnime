package com.gondroid.quoteanime.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class Habit(
    val id: String,
    val title: String,
    val description: String? = null,
    val iconKey: String,
    val colorIndex: Int,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val reminderTime: LocalTime? = null,
    val reminderDays: Set<DayOfWeek> = emptySet(),
    val templateId: String? = null,
    /** Theme key resolved by the presentation layer to a bundled cover image for this habit's card, if any. */
    val coverAnimeSlug: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = 0L
) {
    /** True when [date] falls inside the habit's active window. */
    fun isActiveOn(date: LocalDate): Boolean =
        !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate))
}
