package com.gondroid.quoteanime.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gondroid.quoteanime.data.local.db.dao.FavoriteQuoteDao
import com.gondroid.quoteanime.data.local.db.dao.HabitCompletionDao
import com.gondroid.quoteanime.data.local.db.dao.HabitDao
import com.gondroid.quoteanime.data.local.db.entity.FavoriteQuoteEntity
import com.gondroid.quoteanime.data.local.db.entity.HabitCompletionEntity
import com.gondroid.quoteanime.data.local.db.entity.HabitEntity

@Database(
    entities = [
        FavoriteQuoteEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteQuoteDao(): FavoriteQuoteDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
}
