package com.miletracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps [Dispatchers.Main] for a [TestDispatcher] for the duration of each test.
 *
 * ViewModels launch into `viewModelScope`, which is hard-wired to `Dispatchers.Main.immediate`;
 * without this rule any ViewModel test would throw on the first `launch`.
 *
 * Defaults to [StandardTestDispatcher] so coroutines do NOT run eagerly — each test must
 * advance the scheduler (e.g. `advanceUntilIdle()`), which makes asynchrony explicit and
 * lets tests assert on intermediate states (loading, pre-restore) deterministically.
 * `runTest` detects the main dispatcher's scheduler automatically, so test bodies and
 * `viewModelScope` share a single virtual clock.
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
