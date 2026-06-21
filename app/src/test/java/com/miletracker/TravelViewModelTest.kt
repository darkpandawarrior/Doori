package com.miletracker

import app.cash.turbine.test
import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.travel.model.TripStatus
import com.miletracker.feature.travel.repository.TravelRepository
import com.miletracker.feature.travel.viewmodel.TravelAction
import com.miletracker.feature.travel.viewmodel.TravelData
import com.miletracker.feature.travel.viewmodel.TravelEffect
import com.miletracker.feature.travel.viewmodel.TravelViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * H: behavioural coverage for [TravelViewModel], the booking-hub reducer. The repository is a concrete
 * in-memory mock (no deps), so we drive the real one.
 */
class TravelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = TravelViewModel(TravelRepository())

    @Test
    fun `init loads the active booking and only upcoming bookings`() {
        val content = viewModel().state.value.content
        assertTrue(content is ScreenState.Content<TravelData>)
        val data = (content as ScreenState.Content<TravelData>).data
        assertEquals(TripStatus.ACTIVE, data.activeBooking?.status)
        assertTrue(data.upcoming.isNotEmpty())
        assertTrue(data.upcoming.all { it.status == TripStatus.UPCOMING })
    }

    @Test
    fun `ViewBoardingPass emits an informational message effect`() = runTest {
        val vm = viewModel()
        vm.effect.test {
            vm.onAction(TravelAction.ViewBoardingPass)
            assertTrue(awaitItem() is TravelEffect.ShowMessage)
        }
    }

    @Test
    fun `BookFlight is illustrative and emits a message effect`() = runTest {
        val vm = viewModel()
        vm.effect.test {
            vm.onAction(TravelAction.BookFlight)
            assertTrue(awaitItem() is TravelEffect.ShowMessage)
        }
    }

    @Test
    fun `Refresh re-loads content`() {
        val vm = viewModel()
        vm.onAction(TravelAction.Refresh)
        assertTrue(vm.state.value.content is ScreenState.Content<TravelData>)
    }
}
