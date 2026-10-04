package com.gondroid.quoteanime.di

import com.gondroid.quoteanime.domain.model.DefaultHabitTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumGateTest {

    private val paid = PremiumGate(paymentsEnabled = true)
    private val free = PremiumGate(paymentsEnabled = false)

    @Test
    fun `given payments on, when the user is not premium, then the free limit applies`() {
        assertEquals(PremiumGate.FREE_HABIT_LIMIT, paid.maxActiveHabits(isPremium = false))
        assertEquals(PremiumGate.UNLIMITED_HABITS, paid.maxActiveHabits(isPremium = true))
    }

    @Test
    fun `given payments off, when the user is not premium, then habits are unlimited`() {
        assertEquals(PremiumGate.UNLIMITED_HABITS, free.maxActiveHabits(isPremium = false))
    }

    @Test
    fun `given payments on, when templates are filtered, then the list is untouched`() {
        assertSame(DefaultHabitTemplates.ALL, paid.availableTemplates(DefaultHabitTemplates.ALL))
    }

    @Test
    fun `given payments off, when templates are filtered, then none stays premium-only`() {
        val templates = free.availableTemplates(DefaultHabitTemplates.ALL)

        assertEquals(DefaultHabitTemplates.ALL.map { it.id }, templates.map { it.id })
        assertTrue(templates.none { it.isPremiumOnly })
    }

    @Test
    fun `the shipped build keeps payments off`() {
        // Flip this test together with PAYMENTS_ENABLED once the subscription exists in Play Console.
        assertEquals(false, PremiumGate().paymentsEnabled)
    }
}
