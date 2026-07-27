package com.gondroid.quoteanime.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the habit tracking tables. Favorites are left untouched: a destructive
 * fallback here would wipe every installed user's saved quotes.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `habits` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `iconKey` TEXT NOT NULL,
                `colorIndex` INTEGER NOT NULL,
                `startDate` TEXT NOT NULL,
                `endDate` TEXT,
                `reminderHour` INTEGER,
                `reminderMinute` INTEGER,
                `reminderDays` TEXT NOT NULL,
                `templateId` TEXT,
                `isArchived` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `habit_completions` (
                `habitId` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `completedAt` INTEGER NOT NULL,
                PRIMARY KEY(`habitId`, `date`),
                FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_habit_completions_date` " +
                "ON `habit_completions` (`date`)"
        )
    }
}
