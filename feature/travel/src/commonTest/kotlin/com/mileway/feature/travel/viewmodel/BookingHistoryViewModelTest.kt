package com.mileway.feature.travel.viewmodel

import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.travel.model.BookingType
import com.mileway.feature.travel.model.TravelReqStatus
import com.mileway.feature.travel.repository.TravelHistoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [BookingHistoryViewModel]'s reducer: type-tab selection, status-filter chip, free-text
 * query and their combination, all synchronous (`reload()` runs inline, no `viewModelScope`
 * dispatch), against the real [TravelHistoryRepository] fixed seed (sort order asserted against
 * the same "newest first" ids as `TravelHistoryRepositoryTest`).
 *
 * Note: `reload()` always lands in [ScreenState.Content] (even for an empty result set) - there is
 * no reachable [ScreenState.Error] or [ScreenState.Loading] branch here, since the repository is a
 * synchronous in-memory seed that never throws. That mirrors [TravelViewModel]'s shape.
 */
class BookingHistoryViewModelTest {
    private fun viewModel() = BookingHistoryViewModel(TravelHistoryRepository())

    private fun rows(vm: BookingHistoryViewModel) = (vm.state.value.list as ScreenState.Content).data

    @Test
    fun `All tab loads every booking family newest first`() {
        val vm = viewModel()

        assertEquals(
            listOf("FLT-5001", "MJP-5005", "HTL-5004", "FLT-5002", "VSA-5006", "BUS-5003"),
            rows(vm).map { it.id },
        )
    }

    @Test
    fun `selecting the FLIGHT tab narrows to only flight bookings`() {
        val vm = viewModel()
        val flightTab = BOOKING_HISTORY_TABS.indexOf(BookingType.FLIGHT)

        vm.onAction(BookingHistoryAction.SelectTab(flightTab))

        assertEquals(listOf("FLT-5001", "FLT-5002"), rows(vm).map { it.id })
        assertEquals(flightTab, vm.state.value.tabIndex)
    }

    @Test
    fun `status filter narrows to APPROVED across families`() {
        val vm = viewModel()

        vm.onAction(BookingHistoryAction.SetStatusFilter(TravelReqStatus.APPROVED))

        // HTL-5004 is 5 days old, FLT-5002 is 9 days old - newest first puts the hotel booking ahead.
        assertEquals(listOf("HTL-5004", "FLT-5002"), rows(vm).map { it.id })
    }

    @Test
    fun `type tab and status filter combine and can produce an empty result`() {
        val vm = viewModel()
        val hotelTab = BOOKING_HISTORY_TABS.indexOf(BookingType.HOTEL)

        vm.onAction(BookingHistoryAction.SelectTab(hotelTab))
        vm.onAction(BookingHistoryAction.SetStatusFilter(TravelReqStatus.PENDING))

        // HTL-5004 is the only HOTEL booking and it's APPROVED, never PENDING.
        assertTrue(rows(vm).isEmpty())
    }

    @Test
    fun `query matches by id case-insensitively`() {
        val vm = viewModel()

        vm.onAction(BookingHistoryAction.SetQuery("flt-5001"))

        assertEquals(listOf("FLT-5001"), rows(vm).map { it.id })
    }

    @Test
    fun `query matches by summary substring`() {
        val vm = viewModel()

        vm.onAction(BookingHistoryAction.SetQuery("indigo"))

        assertEquals(listOf("FLT-5001"), rows(vm).map { it.id })
    }

    @Test
    fun `query is trimmed before matching`() {
        val vm = viewModel()

        vm.onAction(BookingHistoryAction.SetQuery("  flt-5001  "))

        assertEquals(listOf("FLT-5001"), rows(vm).map { it.id })
    }

    @Test
    fun `query that matches nothing yields an empty Content list, not an error`() {
        val vm = viewModel()

        vm.onAction(BookingHistoryAction.SetQuery("no-such-booking"))

        assertTrue(rows(vm).isEmpty())
    }

    @Test
    fun `Refresh keeps the current tab, status filter and query applied`() {
        val vm = viewModel()
        vm.onAction(BookingHistoryAction.SetStatusFilter(TravelReqStatus.APPROVED))

        vm.onAction(BookingHistoryAction.Refresh)

        // HTL-5004 is 5 days old, FLT-5002 is 9 days old - newest first puts the hotel booking ahead.
        assertEquals(listOf("HTL-5004", "FLT-5002"), rows(vm).map { it.id })
    }
}
