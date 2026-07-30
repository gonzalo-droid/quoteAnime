package com.gondroid.quoteanime.di

import android.content.Context
import androidx.room.Room
import com.gondroid.quoteanime.data.local.db.AppDatabase
import com.gondroid.quoteanime.data.local.db.MIGRATION_4_5
import com.gondroid.quoteanime.data.local.db.MIGRATION_5_6
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
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
            // Schemas 1-3 predate this app's migration-testing infrastructure and are old/rare
            // enough (long-dormant installs) that a destructive reset on them is acceptable.
            // The 4->5 path must stay non-destructive via MIGRATION_4_5 above: it's the common,
            // recent upgrade path with a large base of real installed users whose favorites we
            // must not silently wipe.
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)
            .build()
    }

    @Provides
    fun provideFavoriteQuoteDao(db: AppDatabase): FavoriteQuoteDao = db.favoriteQuoteDao()

    @Provides
    fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideHabitCompletionDao(db: AppDatabase): HabitCompletionDao = db.habitCompletionDao()
}
