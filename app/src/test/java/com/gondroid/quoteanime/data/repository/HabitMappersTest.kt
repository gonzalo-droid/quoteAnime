package com.gondroid.quoteanime.data.repository

import com.gondroid.quoteanime.domain.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Scenarios covered:
 *  - Full habit with reminder and end date round-trips unchanged
 *  - Habit without reminder stores an empty day list and null time
 *  - Unknown day names in stored data are ignored instead of crashing
 */
class HabitMappersTest {

    private val fullHabit = Habit(
        id = "h1",
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 3,
        startDate = LocalDate.parse("2026-07-01"),
        endDate = LocalDate.parse("2026-08-01"),
        reminderTime = LocalTime.of(7, 30),
        reminderDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        templateId = "train",
        isArchived = false,
        createdAt = 1234L
    )

    @Test
    fun `given a full habit, when mapped both ways, then it stays equal`() {
        val result = fullHabit.toEntity().toDomain()

        assertEquals(fullHabit, result)
    }

    @Test
    fun `given a habit without reminder, when mapped to entity, then time is null and days are empty`() {
        val entity = fullHabit.copy(reminderTime = null, reminderDays = emptySet()).toEntity()

        assertNull(entity.reminderHour)
        assertNull(entity.reminderMinute)
        assertEquals("", entity.reminderDays)
    }

    @Test
    fun `given an unknown day name stored, when mapped to domain, then it is ignored`() {
        val entity = fullHabit.toEntity().copy(reminderDays = "MONDAY,LUNES")

        assertEquals(setOf(DayOfWeek.MONDAY), entity.toDomain().reminderDays)
    }

    @Test
    fun `given a habit without end date, when mapped both ways, then end date stays null`() {
        val openEnded = fullHabit.copy(endDate = null)

        assertNull(openEnded.toEntity().toDomain().endDate)
    }
}
