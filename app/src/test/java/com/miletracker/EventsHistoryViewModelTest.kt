package com.miletracker

import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.events.model.EventStatus
import com.miletracker.feature.events.repository.EventsRepository
import com.miletracker.feature.events.viewmodel.EVENTS_HISTORY_TABS
import com.miletracker.feature.events.viewmodel.EventsHistoryAction
import com.miletracker.feature.events.viewmodel.EventsHistoryViewModel
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** EV (V17): the events-history reducer, All loads everything, a status tab narrows, and the query filters. */
class EventsHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = EventsHistoryViewModel(EventsRepository())

    private fun rows(vm: EventsHistoryViewModel) = (vm.state.value.list as ScreenState.Content).data

    @Test
    fun `All tab loads every event`() {
        val vm = viewModel()
        assertTrue(rows(vm).isNotEmpty())
        assertEquals(EventsRepository().events().size, rows(vm).size)
    }

    @Test
    fun `selecting a status tab narrows to that status`() {
        val vm = viewModel()
        val publishedIndex = EVENTS_HISTORY_TABS.indexOf(EventStatus.PUBLISHED)
        vm.onAction(EventsHistoryAction.SelectTab(publishedIndex))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.status == EventStatus.PUBLISHED })
    }

    @Test
    fun `query filters by title or venue`() {
        val vm = viewModel()
        vm.onAction(EventsHistoryAction.SetQuery("Town Hall"))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.title.contains("Town Hall", ignoreCase = true) })
    }
}
