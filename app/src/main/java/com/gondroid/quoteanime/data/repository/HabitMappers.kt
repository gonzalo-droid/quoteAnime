package com.gondroid.quoteanime.data.repository

import com.gondroid.quoteanime.data.local.db.entity.HabitEntity
import com.gondroid.quoteanime.domain.model.Habit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    title = title,
    iconKey = iconKey,
    colorIndex = colorIndex,
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
    reminderHour = reminderTime?.hour,
    reminderMinute = reminderTime?.minute,
    reminderDays = reminderDays.joinToString(",") { it.name },
    templateId = templateId,
    isArchived = isArchived,
    createdAt = createdAt
)

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    title = title,
    iconKey = iconKey,
    colorIndex = colorIndex,
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
    reminderTime = if (reminderHour != null && reminderMinute != null) {
        LocalTime.of(reminderHour, reminderMinute)
    } else null,
    reminderDays = reminderDays.split(",")
        .mapNotNull { name -> runCatching { DayOfWeek.valueOf(name.trim()) }.getOrNull() }
        .toSet(),
    templateId = templateId,
    isArchived = isArchived,
    createdAt = createdAt
)
