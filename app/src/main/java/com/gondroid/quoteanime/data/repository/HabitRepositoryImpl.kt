package com.gondroid.quoteanime.data.repository

import com.gondroid.quoteanime.data.local.db.dao.HabitCompletionDao
import com.gondroid.quoteanime.data.local.db.dao.HabitDao
import com.gondroid.quoteanime.data.local.db.entity.HabitCompletionEntity
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao
) : HabitRepository {

    override fun getActiveHabits(): Flow<List<Habit>> =
        habitDao.getActive().map { entities -> entities.map { it.toDomain() } }

    override fun getCompletions(habitId: String): Flow<List<LocalDate>> =
        completionDao.getByHabit(habitId).map { rows -> rows.map { LocalDate.parse(it.date) } }

    override fun getAllCompletionDates(): Flow<List<LocalDate>> =
        completionDao.getAllDates().map { dates -> dates.map(LocalDate::parse) }

    override suspend fun countActiveHabits(): Int = habitDao.countActive()

    override suspend fun getHabit(id: String): Habit? = habitDao.getById(id)?.toDomain()

    override suspend fun saveHabit(habit: Habit) = habitDao.upsert(habit.toEntity())

    override suspend fun archiveHabit(id: String) = habitDao.archive(id)

    override suspend fun setCompletion(habitId: String, date: LocalDate, completed: Boolean) {
        if (completed) {
            completionDao.insert(
                HabitCompletionEntity(
                    habitId = habitId,
                    date = date.toString(),
                    completedAt = System.currentTimeMillis()
                )
            )
        } else {
            completionDao.delete(habitId, date.toString())
        }
    }

    override suspend fun isCompleted(habitId: String, date: LocalDate): Boolean =
        completionDao.exists(habitId, date.toString())
}
