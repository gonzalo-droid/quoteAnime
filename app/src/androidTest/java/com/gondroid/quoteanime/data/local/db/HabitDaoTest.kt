package com.gondroid.quoteanime.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gondroid.quoteanime.data.local.db.dao.HabitCompletionDao
import com.gondroid.quoteanime.data.local.db.dao.HabitDao
import com.gondroid.quoteanime.data.local.db.entity.HabitCompletionEntity
import com.gondroid.quoteanime.data.local.db.entity.HabitEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var completionDao: HabitCompletionDao

    private fun habit(id: String, archived: Boolean = false) = HabitEntity(
        id = id,
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = "2026-07-01",
        endDate = null,
        reminderHour = null,
        reminderMinute = null,
        reminderDays = "",
        templateId = null,
        isArchived = archived,
        createdAt = 1000
    )

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        habitDao = db.habitDao()
        completionDao = db.habitCompletionDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun archivedHabitsAreExcludedFromActive() = runTest {
        habitDao.upsert(habit("h1"))
        habitDao.upsert(habit("h2", archived = true))

        assertEquals(listOf("h1"), habitDao.getActive().first().map { it.id })
        assertEquals(1, habitDao.countActive())
    }

    @Test
    fun getArchivedReturnsOnlyArchivedHabits() = runTest {
        habitDao.upsert(habit("h1"))
        habitDao.upsert(habit("h2", archived = true))

        assertEquals(listOf("h2"), habitDao.getArchived().first().map { it.id })
    }

    @Test
    fun unarchiveMovesTheHabitBackToActive() = runTest {
        habitDao.upsert(habit("h1", archived = true))

        habitDao.unarchive("h1")

        assertEquals(listOf("h1"), habitDao.getActive().first().map { it.id })
        assertTrue(habitDao.getArchived().first().isEmpty())
    }

    @Test
    fun deletingAHabitCascadesToItsCompletions() = runTest {
        habitDao.upsert(habit("h1"))
        completionDao.insert(HabitCompletionEntity("h1", "2026-07-25", 1))

        habitDao.delete("h1")

        assertEquals(null, habitDao.getById("h1"))
        assertTrue(completionDao.getByHabit("h1").first().isEmpty())
    }

    @Test
    fun insertingSameDayTwiceKeepsOneRow() = runTest {
        habitDao.upsert(habit("h1"))
        completionDao.insert(HabitCompletionEntity("h1", "2026-07-25", 1))
        completionDao.insert(HabitCompletionEntity("h1", "2026-07-25", 2))

        assertEquals(1, completionDao.getByHabit("h1").first().size)
        assertTrue(completionDao.exists("h1", "2026-07-25"))
    }

    @Test
    fun deletingCompletionRemovesTheDay() = runTest {
        habitDao.upsert(habit("h1"))
        completionDao.insert(HabitCompletionEntity("h1", "2026-07-25", 1))

        completionDao.delete("h1", "2026-07-25")

        assertFalse(completionDao.exists("h1", "2026-07-25"))
    }

    @Test
    fun distinctDatesAreReturnedOnceAcrossHabits() = runTest {
        habitDao.upsert(habit("h1"))
        habitDao.upsert(habit("h2"))
        completionDao.insert(HabitCompletionEntity("h1", "2026-07-25", 1))
        completionDao.insert(HabitCompletionEntity("h2", "2026-07-25", 1))
        completionDao.insert(HabitCompletionEntity("h2", "2026-07-24", 1))

        assertEquals(listOf("2026-07-25", "2026-07-24"), completionDao.getAllDates().first())
    }
}
