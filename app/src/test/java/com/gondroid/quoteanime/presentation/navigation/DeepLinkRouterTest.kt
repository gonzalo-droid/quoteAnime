package com.gondroid.quoteanime.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Scenarios covered:
 *  - The intent extras resolve to the right link (a quote wins over Mi Rutina)
 *  - Cold start, onboarding complete: the splash routes to Home and the link is applied directly,
 *    on top of Home so "back" returns to it
 *  - Cold start, onboarding incomplete: the link waits through the onboarding and is applied
 *    when it finishes
 *  - A tap while the onboarding is on screen also waits
 *  - A tap with the app already in its main content is applied right away
 *  - Without a link the app just lands on Home
 *
 * The splash's own decision (Home vs onboarding) is covered by SplashViewModelTest.
 */
class DeepLinkRouterTest {

    private val home = Screen.Home.route
    private val routine = Screen.Routine.route

    @Test
    fun `given the intent extras, when resolved, then a quote wins over Mi Rutina`() {
        assertEquals(AppDeepLink.Quote("45"), AppDeepLink.from(quoteId = "45", openRoutine = true))
        assertEquals(AppDeepLink.Routine, AppDeepLink.from(quoteId = null, openRoutine = true))
        assertNull(AppDeepLink.from(quoteId = null, openRoutine = false))
    }

    @Test
    fun `given a reminder cold start and a completed onboarding, when the splash routes to Home, then Mi Rutina opens on top of Home`() {
        val router = DeepLinkRouter(AppDeepLink.Routine)

        assertEquals(listOf(home, routine), router.enterMain())
        assertNull(router.pending)
    }

    @Test
    fun `given a quote widget cold start, when the app reaches Home, then Home opens positioned on that quote`() {
        val router = DeepLinkRouter(AppDeepLink.Quote("45"))

        assertEquals(listOf("home?quoteId=45"), router.enterMain())
    }

    @Test
    fun `given an incomplete onboarding, when a link is pending, then it waits and is applied once the onboarding finishes`() {
        val router = DeepLinkRouter(AppDeepLink.Routine)

        // The splash routed to the onboarding: nothing consumes the link meanwhile.
        assertEquals(AppDeepLink.Routine, router.pending)

        // Onboarding finished.
        assertEquals(listOf(home, routine), router.enterMain())
        assertNull(router.pending)
    }

    @Test
    fun `given the onboarding on screen, when a link arrives, then it is parked until the onboarding finishes`() {
        val router = DeepLinkRouter()

        assertNull(router.open(AppDeepLink.Routine, isMainReady = false))
        assertEquals(AppDeepLink.Routine, router.pending)

        assertEquals(listOf(home, routine), router.enterMain())
    }

    @Test
    fun `given the app in its main content, when a link arrives, then it is applied right away`() {
        val router = DeepLinkRouter()

        assertEquals(listOf(home, routine), router.open(AppDeepLink.Routine, isMainReady = true))
        assertEquals(listOf("home?quoteId=7"), router.open(AppDeepLink.Quote("7"), isMainReady = true))
        assertNull(router.pending)
    }

    @Test
    fun `given no link, when the app reaches Home, then only Home is shown`() {
        assertEquals(listOf(home), DeepLinkRouter().enterMain())
    }
}
