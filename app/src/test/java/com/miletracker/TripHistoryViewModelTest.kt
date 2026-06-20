package com.miletracker

import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.travel.model.TravelReqStatus
import com.miletracker.feature.travel.repository.TravelHistoryRepository
import com.miletracker.feature.travel.viewmodel.TRIP_HISTORY_TABS
import com.miletracker.feature.travel.viewmodel.TripHistoryAction
import com.miletracker.feature.travel.viewmodel.TripHistoryViewModel
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TR.8 (V17): the trip-history reducer over the offline fake — All loads every trip, a status tab narrows, and
 * the query filters. Proves the F0 HistoryListScaffold MVI contract for trip requests.
 */
class TripHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = TripHistoryViewModel(TravelHistoryRepository())

    private fun rows(vm: TripHistoryViewModel) = (vm.state.value.list as ScreenState.Content).data

    @Test
    fun `All tab loads every trip`() {
        val vm = viewModel()
        assertTrue(rows(vm).isNotEmpty())
        assertEquals(TravelHistoryRepository().trips().size, rows(vm).size)
    }

    @Test
    fun `selecting a status tab narrows to that status`() {
        val vm = viewModel()
        val approvedIndex = TRIP_HISTORY_TABS.indexOf(TravelReqStatus.APPROVED)
        vm.onAction(TripHistoryAction.SelectTab(approvedIndex))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.status == TravelReqStatus.APPROVED })
    }

    @Test
    fun `query filters by purpose or route`() {
        val vm = viewModel()
        vm.onAction(TripHistoryAction.SetQuery("Bengaluru"))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.route.contains("Bengaluru", ignoreCase = true) })
    }
}
