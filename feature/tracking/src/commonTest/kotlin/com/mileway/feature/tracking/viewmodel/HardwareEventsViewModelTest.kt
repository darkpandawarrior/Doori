package com.mileway.feature.tracking.viewmodel

import com.mileway.core.data.dao.HardwareEventDao
import com.mileway.core.data.model.db.EventAudience
import com.mileway.core.data.model.db.EventType
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.feature.tracking.repository.HardwareEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [HardwareEventsViewModel]'s list/filter state machine.
 *
 * The fake is named [EventLogDao] rather than the obvious `FakeHardwareEventDao` on purpose: a
 * `private class FakeHardwareEventDao` already exists in `SmartEventLoggerTest.kt` in this same
 * package, and a same-named declaration here is a compile-time redeclaration clash. An earlier
 * attempt at this file hit exactly that and had to be dropped.
 */
internal class EventLogDao(
    private val stored: List<HardwareEvent> = emptyList(),
    private val failOnRead: Boolean = false,
) : HardwareEventDao {
    override suspend fun getEventsByToken(token: String): List<HardwareEvent> {
        if (failOnRead) error("dao unavailable")
        return stored
    }

    // Everything below is unused by HardwareEventsViewModel — the ViewModel only reads through
    // HardwareEventRepository.getEventsForRoute. Present because the interface is wide.
    override suspend fun insert(event: HardwareEvent): Long = 0L

    override suspend fun insertAll(events: List<HardwareEvent>): List<Long> = events.map { 0L }

    override suspend fun insertEvents(events: List<HardwareEvent>) = Unit

    override fun observeEventsByToken(token: String): Flow<List<HardwareEvent>> = flowOf(stored)

    override suspend fun getEventsByTokenAndTypes(
        token: String,
        types: List<EventType>,
    ): List<HardwareEvent> = stored

    override suspend fun getEventsByTokenAndAudience(
        token: String,
        audiences: List<EventAudience>,
    ): List<HardwareEvent> = stored

    override suspend fun getEventsWithLocationByToken(token: String): List<HardwareEvent> = stored

    override suspend fun getEventsByTokenAndTimeRange(
        token: String,
        startTime: Long,
        endTime: Long,
    ): List<HardwareEvent> = stored

    override suspend fun getEventCountByToken(token: String): Int = stored.size

    override suspend fun getEventCountByTokenAndType(
        token: String,
        eventType: EventType,
    ): Int = stored.count { it.eventType == eventType }

    override suspend fun deleteEventsOlderThan(cutoffTime: Long): Int = 0

    override suspend fun deleteEventsByToken(token: String): Int = 0

    override suspend fun deleteByToken(token: String) = Unit

    override suspend fun getUnsyncedEvents(limit: Int): List<HardwareEvent> = emptyList()

    override suspend fun getUnsyncedEventsByToken(token: String): List<HardwareEvent> = emptyList()

    override suspend fun markEventsAsUploaded(ids: List<Long>): Int = 0

    override suspend fun markEventsAsSynced(eventIds: List<Long>) = Unit

    override suspend fun getRecentEvents(limit: Int): List<HardwareEvent> = stored.take(limit)

    override suspend fun getDistinctEventTypesByToken(token: String): List<EventType> = stored.map { it.eventType }.distinct()
}

class HardwareEventsViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun event(
        id: Long,
        text: String,
        audience: EventAudience = EventAudience.USER,
        activity: String? = null,
        time: Long = 1_000L,
    ) = HardwareEvent(
        id = id,
        token = TOKEN,
        eventType = EventType.TRACKING_STARTED,
        event = text,
        time = time,
        audience = audience,
        activity = activity,
    )

    private fun viewModel(
        stored: List<HardwareEvent> = emptyList(),
        failOnRead: Boolean = false,
    ) = HardwareEventsViewModel(HardwareEventRepository(EventLogDao(stored, failOnRead)))

    @Test
    fun `starts empty and not loading`() =
        runTest {
            val state = viewModel().state.value
            assertEquals(emptyList(), state.allEvents)
            assertTrue(!state.isLoading)
        }

    @Test
    fun `LoadByToken publishes the stored rows and clears the loading flag`() =
        runTest {
            val stored = listOf(event(1, "Tracking started"), event(2, "Tracking stopped"))
            val vm = viewModel(stored)

            vm.onAction(HardwareEventsAction.LoadByToken(TOKEN))

            val state = vm.state.value
            assertEquals(listOf(1L, 2L), state.allEvents.map { it.id })
            assertTrue(!state.isLoading)
        }

    @Test
    fun `an empty dao falls back to the demo seed rather than showing nothing`() =
        runTest {
            val vm = viewModel(stored = emptyList())

            vm.onAction(HardwareEventsAction.LoadByToken(TOKEN))

            // Deliberate product behaviour: an empty log renders the demo seed, not a blank screen.
            assertTrue(vm.state.value.allEvents.isNotEmpty())
        }

    @Test
    fun `a dao failure also falls back to the demo seed and clears loading`() =
        runTest {
            val vm = viewModel(failOnRead = true)

            vm.onAction(HardwareEventsAction.LoadByToken(TOKEN))

            val state = vm.state.value
            assertTrue(state.allEvents.isNotEmpty(), "error path must not strand the UI empty")
            assertTrue(!state.isLoading, "error path must clear isLoading or the spinner never stops")
        }

    @Test
    fun `search matches the event text case-insensitively`() =
        runTest {
            val vm = viewModel(listOf(event(1, "Tracking STARTED"), event(2, "Battery low")))
            vm.onAction(HardwareEventsAction.LoadByToken(TOKEN))

            vm.onAction(HardwareEventsAction.SetSearchQuery("started"))

            assertEquals(listOf(1L), vm.state.value.filteredEvents.map { it.id })
        }

    @Test
    fun `search also matches the activity field`() =
        runTest {
            val vm =
                viewModel(
                    listOf(
                        event(1, "Tracking started", activity = "IN_VEHICLE"),
                        event(2, "Tracking started", activity = "WALKING"),
                    ),
                )
            vm.onAction(HardwareEventsAction.LoadByToken(TOKEN))

            vm.onAction(HardwareEventsAction.SetSearchQuery("vehicle"))

            assertEquals(listOf(1L), vm.state.value.filteredEvents.map { it.id })
        }

    @Test
    fun `audience filter defaults to USER so SUPPORT rows are hidden`() =
        runTest {
            val vm =
                viewModel(
                    listOf(
                        event(1, "User event", audience = EventAudience.USER),
                        event(2, "Support event", audience = EventAudience.SUPPORT),
                    ),
                )

            vm.onAction(HardwareEventsAction.LoadByToken(TOKEN))

            assertEquals(listOf(1L), vm.state.value.filteredEvents.map { it.id })
        }

    @Test
    fun `deselecting the last audience shows everything, not nothing`() =
        runTest {
            val vm =
                viewModel(
                    listOf(
                        event(1, "User event", audience = EventAudience.USER),
                        event(2, "Support event", audience = EventAudience.SUPPORT),
                    ),
                )
            vm.onAction(HardwareEventsAction.LoadByToken(TOKEN))

            // Toggling USER off leaves the set empty, which the filter treats as "no restriction".
            // Worth pinning: the alternative reading (empty set = match nothing) would silently
            // blank the screen.
            vm.onAction(HardwareEventsAction.ToggleAudienceFilter(EventAudience.USER))

            assertEquals(setOf<EventAudience>(), vm.state.value.selectedAudiences)
            assertEquals(listOf(1L, 2L), vm.state.value.filteredEvents.map { it.id })
        }

    @Test
    fun `ClearFilters restores the default audience and drops the query`() =
        runTest {
            val vm =
                viewModel(
                    listOf(
                        event(1, "User event", audience = EventAudience.USER),
                        event(2, "Support event", audience = EventAudience.SUPPORT),
                    ),
                )
            vm.onAction(HardwareEventsAction.LoadByToken(TOKEN))
            vm.onAction(HardwareEventsAction.SetSearchQuery("system"))
            vm.onAction(HardwareEventsAction.ToggleAudienceFilter(EventAudience.SUPPORT))

            vm.onAction(HardwareEventsAction.ClearFilters)

            val state = vm.state.value
            assertEquals("", state.searchQuery)
            assertEquals(setOf(EventAudience.USER), state.selectedAudiences)
            assertEquals(listOf(1L), state.filteredEvents.map { it.id })
        }

    @Test
    fun `stats are computed over all events, not the filtered view`() =
        runTest {
            val vm =
                viewModel(
                    listOf(
                        event(1, "User event", audience = EventAudience.USER, time = 500L),
                        event(2, "Support event", audience = EventAudience.SUPPORT, time = 900L),
                    ),
                )
            vm.onAction(HardwareEventsAction.LoadByToken(TOKEN))

            // The default USER filter hides row 2, but the stats header must still report both —
            // otherwise the count changes every time the user types in the search box.
            vm.onAction(HardwareEventsAction.SetSearchQuery("user"))

            val stats = vm.state.value.eventStats
            assertEquals(2, stats.totalCount)
            assertEquals(1, stats.audienceCounts[EventAudience.SUPPORT])
            assertEquals(500L to 900L, stats.timeRange)
        }

    @Test
    fun `prepareExportPayload returns null when the filtered list is empty`() =
        runTest {
            val vm = viewModel(listOf(event(1, "User event")))
            vm.onAction(HardwareEventsAction.LoadByToken(TOKEN))
            vm.onAction(HardwareEventsAction.SetSearchQuery("nothing matches this"))

            assertEquals(null, vm.prepareExportPayload(ExportFormat.CSV))
        }

    private companion object {
        const val TOKEN = "route-1"
    }
}
