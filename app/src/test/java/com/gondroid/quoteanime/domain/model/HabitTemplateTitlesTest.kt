package com.gondroid.quoteanime.domain.model

import com.gondroid.quoteanime.domain.model.HabitTemplateTitles.Title
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Scenarios covered:
 *  - A literal title is shown as is (trimmed), including a one-word lowercase one
 *  - A known key resolves to a key the presentation layer localizes
 *  - An unknown key, or a blank title, can't be shown (the template is dropped)
 *  - Every bundled template's title is a known key
 */
class HabitTemplateTitlesTest {

    @Test
    fun `given a literal title, when parsed, then it is shown as is`() {
        assertEquals(Title.Literal("Leer 20 minutos"), HabitTemplateTitles.parse("Leer 20 minutos"))
        assertEquals(Title.Literal("Leer"), HabitTemplateTitles.parse("  Leer "))
        assertEquals(Title.Literal("leer"), HabitTemplateTitles.parse("leer"))
        assertEquals(Title.Literal("Sé un saiyan"), HabitTemplateTitles.parse("Sé un saiyan"))
    }

    @Test
    fun `given a known key, when parsed, then it resolves as a key`() {
        assertEquals(Title.Key("template_theme_ninja"), HabitTemplateTitles.parse("template_theme_ninja"))
        assertEquals(Title.Key("template_read"), HabitTemplateTitles.parse(" template_read "))
    }

    @Test
    fun `given an unknown key, when parsed, then the title can't be shown`() {
        assertNull(HabitTemplateTitles.parse("template_theme_bleach"))
        assertNull(HabitTemplateTitles.parse("habit_x"))
        assertNull(HabitTemplateTitles.parse("   "))
    }

    @Test
    fun `given the bundled templates, when checked, then every title is a known key`() {
        DefaultHabitTemplates.ALL.forEach { template ->
            assertEquals(Title.Key(template.title), HabitTemplateTitles.parse(template.title))
        }
    }
}
