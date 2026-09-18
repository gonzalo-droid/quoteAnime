package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Scenarios covered:
 *  - A selection saved with emotions only is cleaned to empty (every anime) and saved
 *  - A mixed selection keeps only the animes, and that is saved
 *  - A valid selection is returned without writing
 *  - An unknown anime list (not loaded) leaves the selection alone
 *  - A failed write still returns the valid selection
 */
class ReconcileAnimeSelectionUseCaseTest {

    private lateinit var repository: UserPreferencesRepository
    private lateinit var useCase: ReconcileAnimeSelectionUseCase
    private val animes = listOf("Bleach", "Naruto", "One Piece")

    @Before
    fun setup() {
        repository = mockk()
        coJustRun { repository.updateSelectedCategories(any()) }
        useCase = ReconcileAnimeSelectionUseCase(repository)
    }

    @Test
    fun `given only emotions were saved, then the selection is cleared and saved`() = runTest {
        assertEquals(emptySet<String>(), useCase(setOf("motivación", "reflexión"), animes))
        coVerify(exactly = 1) { repository.updateSelectedCategories(emptySet()) }
    }

    @Test
    fun `given animes and emotions were saved, then only the animes are kept and saved`() = runTest {
        assertEquals(setOf("Naruto"), useCase(setOf("Naruto", "motivación"), animes))
        coVerify(exactly = 1) { repository.updateSelectedCategories(setOf("Naruto")) }
    }

    @Test
    fun `given a valid selection, then nothing is written`() = runTest {
        assertEquals(setOf("Naruto", "Bleach"), useCase(setOf("Naruto", "Bleach"), animes))
        coVerify(exactly = 0) { repository.updateSelectedCategories(any()) }
    }

    @Test
    fun `given the anime list is not loaded, then the selection is left alone`() = runTest {
        assertEquals(setOf("motivación"), useCase(setOf("motivación"), emptyList()))
        coVerify(exactly = 0) { repository.updateSelectedCategories(any()) }
    }

    @Test
    fun `given the write fails, then the valid selection is still applied`() = runTest {
        coEvery { repository.updateSelectedCategories(any()) } throws java.io.IOException("disk full")
        assertEquals(emptySet<String>(), useCase(setOf("motivación"), animes))
    }
}
