package com.gondroid.quoteanime.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gondroid.quoteanime.data.local.db.dao.FavoriteQuoteDao
import com.gondroid.quoteanime.data.local.db.entity.FavoriteQuoteEntity

@Database(
    entities = [FavoriteQuoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteQuoteDao(): FavoriteQuoteDao
}
