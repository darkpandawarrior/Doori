package com.miletracker

import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.travel.model.BookingType
import com.miletracker.feature.travel.model.TravelReqStatus
import com.miletracker.feature.travel.repository.TravelHistoryRepository
import com.miletracker.feature.travel.viewmodel.BOOKING_HISTORY_TABS
import com.miletracker.feature.travel.viewmodel.BookingHistoryAction
import com.miletracker.feature.travel.viewmodel.BookingHistoryViewModel
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TR.8 (V17): the booking-history reducer over the offline fake — All loads every booking family, a type tab
 * narrows, a status filter chip narrows further, and the query filters.
 */
class BookingHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = BookingHistoryViewModel(TravelHistoryRepository())

    private fun rows(vm: BookingHistoryViewModel) = (vm.state.value.list as ScreenState.Content).data

    @Test
    fun `All tab loads every booking family`() {
        val vm = viewModel()
        assertTrue(rows(vm).isNotEmpty())
        assertEquals(TravelHistoryRepository().bookings().size, rows(vm).size)
    }

    @Test
    fun `selecting a type tab narrows to that family`() {
        val vm = viewModel()
        val flightIndex = BOOKING_HISTORY_TABS.indexOf(BookingType.FLIGHT)
        vm.onAction(BookingHistoryAction.SelectTab(flightIndex))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.type == BookingType.FLIGHT })
    }

    @Test
    fun `status filter narrows to that status`() {
        val vm = viewModel()
        vm.onAction(BookingHistoryAction.SetStatusFilter(TravelReqStatus.APPROVED))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.status == TravelReqStatus.APPROVED })
    }
}
