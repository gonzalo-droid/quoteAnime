package com.gondroid.quoteanime.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards against data loss: users upgrading from version 5 must keep their existing
 * habits, and the two new columns (description, coverAnimeSlug) must be nullable so
 * pre-existing rows don't need a value for them.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom5To6Test {

    private val testDb = "migration_test_db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate5To6_keepsExistingHabitsAndAddsNullableColumns() {
        helper.createDatabase(testDb, 5).apply {
            execSQL(
                """
                INSERT INTO habits (
                    id, title, iconKey, colorIndex, startDate, endDate,
                    reminderHour, reminderMinute, reminderDays, templateId,
                    isArchived, createdAt
                ) VALUES (
                    'h1', 'Leer', 'book', 1, '2026-07-01', NULL,
                    NULL, NULL, '', NULL,
                    0, 1000
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 6, true, MIGRATION_5_6)

        db.query("SELECT id, title, description, coverAnimeSlug FROM habits").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("h1", cursor.getString(0))
            assertEquals("Leer", cursor.getString(1))
            assertNull(cursor.getString(2))
            assertNull(cursor.getString(3))
        }
        db.query(
            "SELECT name FROM pragma_table_info('habits') WHERE name IN ('description', 'coverAnimeSlug')"
        ).use {
            assertTrue(it.count == 2)
        }
    }
}
