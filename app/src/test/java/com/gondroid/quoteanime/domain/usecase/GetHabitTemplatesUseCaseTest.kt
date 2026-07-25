package com.gondroid.quoteanime.domain.usecase

import app.cash.turbine.test
import com.gondroid.quoteanime.data.remote.HabitTemplateRemoteDataSource
import com.gondroid.quoteanime.domain.model.DefaultHabitTemplates
import com.gondroid.quoteanime.domain.model.HabitTemplate
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Scenarios covered:
 *  - Remote templates are returned sorted by order
 *  - An empty remote node falls back to the bundled defaults
 *  - A remote failure falls back to the bundled defaults
 */
class GetHabitTemplatesUseCaseTest {

    private lateinit var remote: HabitTemplateRemoteDataSource
    private lateinit var useCase: GetHabitTemplatesUseCase

    @Before
    fun setup() {
        remote = mockk()
        useCase = GetHabitTemplatesUseCase(remote)
    }

    @Test
    fun `given remote templates, when collected, then they are sorted by order`() = runTest {
        every { remote.getTemplates() } returns flowOf(
            listOf(
                HabitTemplate("b", "Leer", "book", order = 2),
                HabitTemplate("a", "Entrenar", "dumbbell", order = 1)
            )
        )

        useCase().test {
            assertEquals(listOf("a", "b"), awaitItem().map { it.id })
            awaitComplete()
        }
    }

    @Test
    fun `given an empty remote node, when collected, then the bundled defaults are returned`() = runTest {
        every { remote.getTemplates() } returns flowOf(emptyList())

        useCase().test {
            assertEquals(DefaultHabitTemplates.ALL, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given a remote failure, when collected, then the bundled defaults are returned`() = runTest {
        every { remote.getTemplates() } returns flow { throw IllegalStateException("no network") }

        useCase().test {
            assertEquals(DefaultHabitTemplates.ALL, awaitItem())
            awaitComplete()
        }
    }
}
