package com.gondroid.quoteanime.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards against data loss: users upgrading from version 4 must keep their favorites.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom4To5Test {

    private val testDb = "migration_test_db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate4To5_keepsFavoritesAndCreatesHabitTables() {
        helper.createDatabase(testDb, 4).apply {
            execSQL(
                "INSERT INTO favorite_quotes (id, quote, author, anime, animeSlug, categories, savedAt) " +
                    "VALUES ('q1', 'Never give up', 'Naruto', 'Naruto', 'naruto', '', 1000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 5, true, MIGRATION_4_5)

        db.query("SELECT id FROM favorite_quotes").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("q1", cursor.getString(0))
        }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='habits'").use {
            assertTrue(it.count == 1)
        }
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='habit_completions'"
        ).use {
            assertTrue(it.count == 1)
        }
    }
}
