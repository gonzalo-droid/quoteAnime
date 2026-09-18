package com.gondroid.quoteanime.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Scenarios covered:
 *  - A quote widget tap with no quote (loading / error sends an empty id) is no deep link:
 *    the app opens normally, landing on Home, instead of navigating with an empty id
 *  - A blank id doesn't hide a Mi Rutina request carried by the same intent
 *  - A real id still opens that quote
 */
class AppDeepLinkTest {

    @Test
    fun `given an empty or blank quote id, when resolved, then it counts as no deep link`() {
        assertNull(AppDeepLink.from(quoteId = "", openRoutine = false))
        assertNull(AppDeepLink.from(quoteId = "   ", openRoutine = false))
    }

    @Test
    fun `given an empty quote id, when the app starts, then it just lands on Home`() {
        val router = DeepLinkRouter(AppDeepLink.from(quoteId = "", openRoutine = false))

        assertEquals(listOf(Screen.Home.route), router.enterMain())
    }

    @Test
    fun `given an empty quote id and a Mi Rutina request, when resolved, then Mi Rutina opens`() {
        assertEquals(AppDeepLink.Routine, AppDeepLink.from(quoteId = "", openRoutine = true))
    }

    @Test
    fun `given a real quote id, when resolved, then that quote opens`() {
        assertEquals(AppDeepLink.Quote("45"), AppDeepLink.from(quoteId = "45", openRoutine = false))
    }
}
