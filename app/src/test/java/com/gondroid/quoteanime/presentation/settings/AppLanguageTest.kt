package com.gondroid.quoteanime.presentation.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class AppLanguageTest {

    @Test
    fun `given a stored tag, when mapped, then only the language counts`() {
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es-PE"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es_419"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("EN"))
    }

    @Test
    fun `given no tag or a language the app does not ship, when mapped, then it is the system default`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("fr-FR"))
    }

    @Test
    fun `the locale config offers exactly the languages of the picker`() {
        // Unit tests run from the module directory.
        val config = File("src/main/res/xml/locales_config.xml").readText()
        val declared = Regex("""android:name="([^"]+)"""").findAll(config).map { it.groupValues[1] }.toSet()

        assertEquals(AppLanguage.entries.filter { it != AppLanguage.SYSTEM }.map { it.tag }.toSet(), declared)
    }
}
