package com.gondroid.quoteanime.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("date")]
)
data class HabitCompletionEntity(
    val habitId: String,
    val date: String,             // ISO yyyy-MM-dd
    val completedAt: Long
)
