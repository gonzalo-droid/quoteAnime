package com.gondroid.quoteanime.domain.usecase

import app.cash.turbine.test
import com.gondroid.quoteanime.data.remote.HabitTemplateRemoteDataSource
import com.gondroid.quoteanime.domain.model.DefaultHabitTemplates
import com.gondroid.quoteanime.domain.model.HabitTemplate
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Scenarios covered:
 *  - The bundled defaults are emitted immediately, before the remote answers
 *  - Without network (the remote never answers) the bundled defaults stay
 *  - Valid remote templates replace the defaults, sorted by order
 *  - An empty remote node keeps the bundled defaults
 *  - A remote failure keeps the bundled defaults (never an empty list)
 *  - A remote failure after a remote list keeps that list
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
    fun `given a remote that has not answered yet, when collected, then the bundled defaults are emitted immediately`() = runTest {
        // An RTDB listener without network and without disk persistence never fires.
        every { remote.getTemplates() } returns MutableSharedFlow()

        useCase().test {
            assertEquals(DefaultHabitTemplates.ALL, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given remote templates, when they arrive, then they replace the defaults sorted by order`() = runTest {
        val remoteTemplates = MutableSharedFlow<List<HabitTemplate>>()
        every { remote.getTemplates() } returns remoteTemplates

        useCase().test {
            assertEquals(DefaultHabitTemplates.ALL, awaitItem())

            remoteTemplates.emit(
                listOf(
                    HabitTemplate("b", "Leer", "book", order = 2),
                    HabitTemplate("a", "Entrenar", "dumbbell", order = 1)
                )
            )

            assertEquals(listOf("a", "b"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given an empty remote node, when collected, then only the bundled defaults are emitted`() = runTest {
        every { remote.getTemplates() } returns flowOf(emptyList())

        useCase().test {
            assertEquals(DefaultHabitTemplates.ALL, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given a network failure, when collected, then the bundled defaults stay`() = runTest {
        every { remote.getTemplates() } returns flow { throw IllegalStateException("no network") }

        useCase().test {
            assertEquals(DefaultHabitTemplates.ALL, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given a failure after a remote list, when collected, then the remote list stays`() = runTest {
        val remoteTemplates = listOf(HabitTemplate("a", "Entrenar", "dumbbell", order = 1))
        every { remote.getTemplates() } returns flow {
            emit(remoteTemplates)
            throw IllegalStateException("permission revoked")
        }

        useCase().test {
            assertEquals(DefaultHabitTemplates.ALL, awaitItem())
            assertEquals(remoteTemplates, awaitItem())
            awaitComplete()
        }
    }
}
