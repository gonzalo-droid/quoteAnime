package com.gondroid.quoteanime.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val iconKey: String,
    val colorIndex: Int,
    val startDate: String,        // ISO yyyy-MM-dd
    val endDate: String?,         // ISO yyyy-MM-dd, null when open-ended
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val reminderDays: String,     // "MONDAY,WEDNESDAY"; empty when no reminder
    val templateId: String?,
    val isArchived: Boolean,
    val createdAt: Long
)
