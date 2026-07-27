package com.mileway.feature.travel.viewmodel

import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.travel.repository.TravelRepository
import com.siddharth.kmp.common.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [TravelViewModel]'s home-data assembly (active/upcoming split + spend total, sourced from
 * [TravelRepository]'s fixed seed) and its action -> effect mapping.
 *
 * [TravelRepository] is a fixed, non-suspending in-memory seed (no network/DB, never throws), so
 * there is no reachable Loading/Error/Empty branch here: `load()` runs synchronously in `init`, and
 * every seed run always yields one ACTIVE + three UPCOMING bookings. That's a property of the fake,
 * not a gap in these tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TravelViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TravelViewModel(TravelRepository())

    @Test
    fun `initial load assembles active booking, upcoming list and total spend from the repository`() {
        val repository = TravelRepository()
        val vm = TravelViewModel(repository)

        val content = vm.state.value.content as ScreenState.Content
        assertEquals(repository.activeBooking(), content.data.activeBooking)
        assertEquals(repository.upcomingBookings(), content.data.upcoming)
        assertEquals(3, content.data.upcoming.size)
        assertEquals(repository.totalSpend(), content.data.totalSpend)
        assertEquals(17_500.0, content.data.totalSpend)
    }

    @Test
    fun `Refresh recomputes the same data from the fixed seed`() {
        val vm = viewModel()
        val before = (vm.state.value.content as ScreenState.Content).data

        vm.onAction(TravelAction.Refresh)

        val after = vm.state.value.content as ScreenState.Content
        assertEquals(before, after.data)
    }

    @Test
    fun `ViewBoardingPass emits the boarding-pass-unavailable message`() =
        runTest {
            val vm = viewModel()
            val effects = mutableListOf<TravelEffect>()
            val job = launch { effects += vm.effect.first() }

            vm.onAction(TravelAction.ViewBoardingPass)
            advanceUntilIdle()
            job.join()

            assertEquals(
                TravelEffect.ShowMessage(UiText.Dynamic("Boarding pass not available in demo")),
                effects.single(),
            )
        }

    @Test
    fun `BookFlight emits the illustrative-booking message`() =
        runTest {
            val vm = viewModel()
            val effects = mutableListOf<TravelEffect>()
            val job = launch { effects += vm.effect.first() }

            vm.onAction(TravelAction.BookFlight)
            advanceUntilIdle()
            job.join()

            assertEquals(TravelEffect.ShowMessage(UiText.Dynamic("Booking is illustrative.")), effects.single())
        }

    @Test
    fun `BookTrain emits the same illustrative-booking message as BookFlight`() =
        runTest {
            val vm = viewModel()
            val effects = mutableListOf<TravelEffect>()
            val job = launch { effects += vm.effect.first() }

            vm.onAction(TravelAction.BookTrain)
            advanceUntilIdle()
            job.join()

            assertEquals(TravelEffect.ShowMessage(UiText.Dynamic("Booking is illustrative.")), effects.single())
        }
}
