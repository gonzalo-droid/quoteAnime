package com.gondroid.quoteanime.resources

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Each icon's name is what TalkBack announces for its cell in the picker and what the picker's
 * search matches against. Two icons with the same name are two cells a screen-reader user can't
 * tell apart (it happened with `icon_icecream` and `icon_auto_awesome`, both "Treat yourself").
 *
 * Only `icon_*` names are compared; `icon_category_*` are section headers, not icons.
 *
 * Scenarios covered:
 *  - No two icon names are equal in English (`values`)
 *  - No two icon names are equal in Spanish (`values-es`)
 */
class IconLabelUniquenessTest {

    @Test
    fun `given the English icon names, when compared, then no two icons share one`() {
        assertEquals(emptyMap<String, List<String>>(), duplicatedIconNames("values"))
    }

    @Test
    fun `given the Spanish icon names, when compared, then no two icons share one`() {
        assertEquals(emptyMap<String, List<String>>(), duplicatedIconNames("values-es"))
    }

    /** Name (case- and whitespace-insensitive) → the keys that share it, only when more than one. */
    private fun duplicatedIconNames(valuesDir: String): Map<String, List<String>> =
        StringResourceFile.load(valuesDir)
            .filter { it.name.startsWith("icon_") && !it.name.startsWith("icon_category_") }
            .groupBy({ it.text.trim().lowercase() }, { it.name })
            .filterValues { it.size > 1 }
}
