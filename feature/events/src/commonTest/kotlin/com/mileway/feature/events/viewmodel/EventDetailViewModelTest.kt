package com.mileway.feature.events.viewmodel

import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.events.model.EventCategory
import com.mileway.feature.events.model.EventStatus
import com.mileway.feature.events.repository.EventDraft
import com.mileway.feature.events.repository.EventResult
import com.mileway.feature.events.repository.EventsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private class FixedClock(private val epochMs: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(epochMs)
}

/**
 * V29 P29.E: covers [EventDetailViewModel]'s detail load, approve/reject status transitions, the
 * delete guard (only DRAFT/PUBLISHED), edit save, and the linked-expense bulk-link flow, plus
 * [EventsRepository]'s capacity/budget seed data the detail screen's variance chips read from.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun repo() = EventsRepository(FixedClock(1_750_000_000_000L))

    @Test
    fun `loads the seeded event by id`() =
        runTest {
            val repository = repo()
            val seededId = repository.events().first().id
            val vm = EventDetailViewModel(seededId, repository)

            val state = vm.state.value.event as ScreenState.Content
            assertEquals(seededId, state.data.id)
        }

    @Test
    fun `unknown id resolves to an error state`() =
        runTest {
            val vm = EventDetailViewModel("EVT-NOPE", repo())
            assertTrue(vm.state.value.event is ScreenState.Error)
        }

    @Test
    fun `approve flips a pending-approval event to published`() =
        runTest {
            val repository = repo()

            // Only PENDING_APPROVAL rows are meant to be approved/rejected; force one via a NeedsApproval submit.
            var needsApprovalId: String? = null
            repeat(3) {
                val result = repository.submit(EventDraft("New Event", "Venue", "", 10, EventCategory.TECH))
                if (result is EventResult.NeedsApproval) needsApprovalId = result.id
            }
            val pendingId = requireNotNull(needsApprovalId)
            val pendingVm = EventDetailViewModel(pendingId, repository)

            pendingVm.onAction(EventDetailAction.Approve)

            val state = pendingVm.state.value.event as ScreenState.Content
            assertEquals(EventStatus.PUBLISHED, state.data.status)
        }

    @Test
    fun `delete is a no-op for a cancelled event and removes a draft event`() =
        runTest {
            val repository = repo()
            val cancelled = repository.events(EventStatus.CANCELLED).first()
            val draft = repository.events(EventStatus.DRAFT).first()

            val cancelledVm = EventDetailViewModel(cancelled.id, repository)
            cancelledVm.onAction(EventDetailAction.ConfirmDelete)
            assertTrue(repository.get(cancelled.id) != null, "cancelled events aren't deletable")

            val draftVm = EventDetailViewModel(draft.id, repository)
            draftVm.onAction(EventDetailAction.ConfirmDelete)
            assertEquals(null, repository.get(draft.id))
        }

    @Test
    fun `save edit updates category and budget`() =
        runTest {
            val repository = repo()
            val event = repository.events().first()
            val vm = EventDetailViewModel(event.id, repository)

            vm.onAction(EventDetailAction.OpenEdit)
            vm.onAction(EventDetailAction.SetEditCategory(EventCategory.WORKSHOP))
            vm.onAction(EventDetailAction.SetEditBudgetText("500.0"))
            vm.onAction(EventDetailAction.SaveEdit)

            val updated = requireNotNull(repository.get(event.id))
            assertEquals(EventCategory.WORKSHOP, updated.category)
            assertEquals(50_000L, updated.budgetedAmountMinor)
            assertFalse(vm.state.value.showEditSheet)
        }

    @Test
    fun `linking expenses appends to the event and bumps actual cost`() =
        runTest {
            val repository = repo()
            val event = repository.events().first { it.linkedExpenses.isEmpty() }
            val vm = EventDetailViewModel(event.id, repository)

            vm.onAction(EventDetailAction.OpenLinkSheet)
            val candidate = vm.state.value.availableToLink.first()
            vm.onAction(EventDetailAction.ToggleLinkSelection(candidate.id))
            vm.onAction(EventDetailAction.ConfirmLink)

            val updated = requireNotNull(repository.get(event.id))
            assertTrue(updated.linkedExpenses.any { it.id == candidate.id })
            assertEquals(event.actualAmountMinor + candidate.amountMinor, updated.actualAmountMinor)
        }

    @Test
    fun `log expense emits an Event-scoped expense source context`() =
        runTest {
            val repository = repo()
            val event = repository.events().first()
            val vm = EventDetailViewModel(event.id, repository)

            vm.onAction(EventDetailAction.LogExpense)

            val effect = vm.effect.first() as EventDetailEffect.NavigateToExpenseEntry
            val context = effect.context as ExpenseSourceContext.Event
            assertEquals(event.id, context.eventId)
        }
}
