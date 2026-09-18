package com.gondroid.quoteanime.presentation.navigation

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenarios covered:
 *  - A quote widget tap with no quote (loading / error sends an empty id) is no deep link:
 *    the app opens normally, landing on Home, instead of navigating with an empty id
 *  - A blank id doesn't hide a Mi Rutina request carried by the same intent
 *  - A real id still opens that quote
 *  - Relaunch from Recents (FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) repeats no link
 *  - A normal launch with the same extras still opens the link
 *  - The launch flags reuse the running activity (SINGLE_TOP + CLEAR_TOP) and never clear
 *    the task
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

    // ── Relaunch from Recents ─────────────────────────────────────────────────

    @Test
    fun `given the app is reopened from Recents, when resolved, then the old link is ignored`() {
        val fromHistory = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY or AppDeepLink.LAUNCH_FLAGS

        assertNull(AppDeepLink.fromLaunch(quoteId = "45", openRoutine = false, intentFlags = fromHistory))
        assertNull(AppDeepLink.fromLaunch(quoteId = null, openRoutine = true, intentFlags = fromHistory))
    }

    @Test
    fun `given a fresh tap, when resolved, then the link opens`() {
        assertEquals(
            AppDeepLink.Quote("45"),
            AppDeepLink.fromLaunch(quoteId = "45", openRoutine = false, intentFlags = AppDeepLink.LAUNCH_FLAGS)
        )
        assertEquals(
            AppDeepLink.Routine,
            AppDeepLink.fromLaunch(quoteId = null, openRoutine = true, intentFlags = 0)
        )
    }

    // ── Launch flags ──────────────────────────────────────────────────────────

    @Test
    fun `given the launch flags, then they reuse the running activity and keep the task`() {
        val flags = AppDeepLink.LAUNCH_FLAGS
        assertTrue("SINGLE_TOP", flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertTrue("CLEAR_TOP", flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue("NEW_TASK", flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue("no CLEAR_TASK", flags and Intent.FLAG_ACTIVITY_CLEAR_TASK == 0)
    }
}
