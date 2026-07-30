package com.gondroid.quoteanime.domain.repository

import com.gondroid.quoteanime.domain.model.Habit
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun getActiveHabits(): Flow<List<Habit>>
    fun getArchivedHabits(): Flow<List<Habit>>
    fun getCompletions(habitId: String): Flow<List<LocalDate>>
    /** Dates where at least one habit was completed — feeds the global streak. */
    fun getAllCompletionDates(): Flow<List<LocalDate>>
    suspend fun countActiveHabits(): Int
    suspend fun getHabit(id: String): Habit?
    suspend fun saveHabit(habit: Habit)
    suspend fun archiveHabit(id: String)
    suspend fun unarchiveHabit(id: String)
    /** Permanently removes the habit and, via cascade, all of its completions. */
    suspend fun deleteHabit(id: String)
    suspend fun setCompletion(habitId: String, date: LocalDate, completed: Boolean)
    suspend fun isCompleted(habitId: String, date: LocalDate): Boolean
}
