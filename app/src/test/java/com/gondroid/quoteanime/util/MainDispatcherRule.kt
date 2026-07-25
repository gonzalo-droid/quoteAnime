package com.gondroid.quoteanime.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A JUnit rule that replaces [Dispatchers.Main] with a [TestDispatcher] for the duration
 * of each test. Use [StandardTestDispatcher] (default) for explicit coroutine scheduling
 * with [advanceUntilIdle] / [runCurrent], or pass [UnconfinedTestDispatcher] if you want
 * coroutines to execute eagerly without manual advancing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
