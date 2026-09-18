package com.gondroid.quoteanime.presentation.routine

import com.gondroid.quoteanime.domain.model.HabitTemplateTitles
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The domain decides which title keys are shown (anything else is dropped); the presentation
 * layer is what can localize them. The two lists drifting apart would either drop a template
 * the app can name or show a raw key — this pins them together.
 */
class TemplateTitleResourcesTest {

    @Test
    fun `given the known title keys, when compared, then every one has a string resource and no resource is unreachable`() {
        assertEquals(HabitTemplateTitles.KNOWN_KEYS, TEMPLATE_TITLE_RES_BY_KEY.keys)
    }
}
