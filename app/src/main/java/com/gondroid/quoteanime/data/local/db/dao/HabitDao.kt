package com.gondroid.quoteanime.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gondroid.quoteanime.data.local.db.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun getArchived(): Flow<List<HabitEntity>>

    @Query("SELECT COUNT(*) FROM habits WHERE isArchived = 0")
    suspend fun countActive(): Int

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getById(habitId: String): HabitEntity?

    /**
     * Must stay `@Upsert`, never `@Insert(onConflict = REPLACE)`: SQLite's REPLACE deletes the old
     * row before inserting, and that delete cascades to habit_completions, so editing a habit wiped
     * its whole history and streak.
     */
    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Query("UPDATE habits SET isArchived = 1 WHERE id = :habitId")
    suspend fun archive(habitId: String)

    @Query("UPDATE habits SET isArchived = 0 WHERE id = :habitId")
    suspend fun unarchive(habitId: String)

    /** Cascades to habit_completions via HabitCompletionEntity's foreign key. */
    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun delete(habitId: String)
}
