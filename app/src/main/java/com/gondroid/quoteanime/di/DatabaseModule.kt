package com.gondroid.quoteanime.di

import android.content.Context
import androidx.room.Room
import com.gondroid.quoteanime.data.local.db.AppDatabase
import com.gondroid.quoteanime.data.local.db.MIGRATION_4_5
import com.gondroid.quoteanime.data.local.db.dao.FavoriteQuoteDao
import com.gondroid.quoteanime.data.local.db.dao.HabitCompletionDao
import com.gondroid.quoteanime.data.local.db.dao.HabitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "quote_anime_db"
        )
            .addMigrations(MIGRATION_4_5)
            .build()
    }

    @Provides
    fun provideFavoriteQuoteDao(db: AppDatabase): FavoriteQuoteDao = db.favoriteQuoteDao()

    @Provides
    fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideHabitCompletionDao(db: AppDatabase): HabitCompletionDao = db.habitCompletionDao()
}
