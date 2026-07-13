package com.mileway.feature.events.repository

import com.mileway.feature.events.model.EventCategory
import com.mileway.feature.events.model.EventRecord
import com.mileway.feature.events.model.EventStatus
import com.mileway.feature.events.model.LinkedExpense
import kotlin.time.Clock

/** A create-event form payload (EV). */
data class EventDraft(
    val title: String,
    val venue: String,
    val date: String,
    val capacity: Int,
    val category: EventCategory,
)

/** Rotating submission outcome for the create-event flow (EV). */
sealed interface EventResult {
    data class Submitted(val id: String) : EventResult

    data class NeedsApproval(val id: String) : EventResult

    data class PolicyViolation(val messages: List<String>) : EventResult
}

/**
 * Offline fake events store (EV), seeds a deterministic history (Clock-injected, no `Math.random`) and returns
 * a **rotating** [EventResult] (published / approval / policy-violation) across repeated submits. Mirrors the
 * PB/TR/PM fake-repo pattern.
 *
 * V29 P29.E.5: a `NeedsApproval` submission now also persists a real [EventRecord] at
 * [EventStatus.PENDING_APPROVAL] into [events] (not just an id) so the event-detail screen has
 * something to approve/reject. A `Submitted` outcome persists at [EventStatus.PUBLISHED].
 * `PolicyViolation` persists nothing, mirroring the pre-existing "rejected outright" behavior.
 */
class EventsRepository(private val clock: Clock = Clock.System) {
    private val dayMs = 86_400_000L
    private var counter = 0

    /** Small local pool of not-yet-linked expenses, for the P29.E.8 bulk-link picker. */
    private val expensePool: List<LinkedExpense> by lazy {
        val now = clock.now().toEpochMilliseconds()
        listOf(
            LinkedExpense("EXP-9001", "Catering — lunch buffet", 850_00, now - 1 * dayMs),
            LinkedExpense("EXP-9002", "AV equipment rental", 420_00, now - 2 * dayMs),
            LinkedExpense("EXP-9003", "Venue decoration", 260_00, now - 3 * dayMs),
            LinkedExpense("EXP-9004", "Printed collateral", 95_00, now - 4 * dayMs),
            LinkedExpense("EXP-9005", "Local transport for guests", 310_00, now - 5 * dayMs),
        )
    }

    private val events: MutableList<EventRecord> by lazy { seed() }

    fun submit(draft: EventDraft): EventResult {
        val id = "EVT-${3300 + events.size + 1}"
        return when (counter++ % 3) {
            0 -> {
                events.add(0, recordFrom(id, draft, EventStatus.PUBLISHED))
                EventResult.Submitted(id)
            }
            1 -> {
                events.add(0, recordFrom(id, draft, EventStatus.PENDING_APPROVAL))
                EventResult.NeedsApproval(id)
            }
            else ->
                EventResult.PolicyViolation(
                    listOf("Venue capacity exceeds the booking limit", "Budget needs finance approval"),
                )
        }
    }

    fun count(): Int = events.size

    /** All events, or just those in [status] when non-null, newest first. */
    fun events(status: EventStatus? = null): List<EventRecord> = events.filter { status == null || it.status == status }.sortedByDescending { it.dateMillis }

    fun get(id: String): EventRecord? = events.find { it.id == id }

    /** V29 P29.E.5: mock approve/reject — flips a [EventStatus.PENDING_APPROVAL] row to [newStatus]. */
    fun setStatus(
        id: String,
        newStatus: EventStatus,
    ): EventRecord? = updateEvent(id) { it.copy(status = newStatus) }

    /** V29 P29.E.6: edit the event's mutable/current fields; title/venue/date/capacity stay historical/read-only. */
    fun updateEditableFields(
        id: String,
        category: EventCategory,
        budgetedAmountMinor: Long,
    ): EventRecord? = updateEvent(id) { it.copy(category = category, budgetedAmountMinor = budgetedAmountMinor) }

    /** V29 P29.E.7: delete, guarded to [EventRecord.isDeletable] statuses only. */
    fun delete(id: String): Boolean {
        val event = get(id) ?: return false
        if (!event.isDeletable) return false
        return events.removeAll { it.id == id }
    }

    /** Expenses not yet linked to [eventId], for the P29.E.8 bulk-link sheet. */
    fun availableExpensesToLink(eventId: String): List<LinkedExpense> {
        val linkedIds = get(eventId)?.linkedExpenses.orEmpty().map { it.id }.toSet()
        return expensePool.filter { it.id !in linkedIds }
    }

    /** V29 P29.E.8: multi-select bulk-link — appends [expenses] onto the event's linked list. */
    fun linkExpenses(
        id: String,
        expenses: List<LinkedExpense>,
    ): EventRecord? =
        updateEvent(id) { current ->
            val merged = (current.linkedExpenses + expenses).distinctBy { it.id }
            current.copy(
                linkedExpenses = merged,
                actualAmountMinor = current.actualAmountMinor + expenses.sumOf { it.amountMinor },
            )
        }

    private fun updateEvent(
        id: String,
        transform: (EventRecord) -> EventRecord,
    ): EventRecord? {
        val index = events.indexOfFirst { it.id == id }
        if (index < 0) return null
        val updated = transform(events[index])
        events[index] = updated
        return updated
    }

    private fun recordFrom(
        id: String,
        draft: EventDraft,
        status: EventStatus,
    ): EventRecord =
        EventRecord(
            id = id,
            title = draft.title,
            venue = draft.venue,
            category = draft.category,
            capacity = draft.capacity,
            actualAttendance = 0,
            status = status,
            dateMillis = clock.now().toEpochMilliseconds(),
        )

    private fun seed(): MutableList<EventRecord> {
        val now = clock.now().toEpochMilliseconds()
        val spec =
            listOf(
                Spec("Q3 Town Hall", "Auditorium A", EventCategory.ALL_HANDS, 320, 298, EventStatus.PUBLISHED, 2L, 250_000_00, 231_400_00),
                Spec("Android Guild Meetup", "Pune Office", EventCategory.TECH, 45, 41, EventStatus.COMPLETED, 12L, 18_000_00, 16_450_00),
                Spec("Diwali Celebration", "Terrace", EventCategory.CULTURE, 180, 0, EventStatus.DRAFT, 5L, 60_000_00, 0L),
                Spec("Sales Kickoff 2026", "Grand Hyatt", EventCategory.OFFSITE, 95, 88, EventStatus.CANCELLED, 20L, 120_000_00, 42_000_00),
                Spec("Design Sprint", "BKC Hub", EventCategory.WORKSHOP, 24, 24, EventStatus.PUBLISHED, 1L, 8_000_00, 7_600_00),
            )
        return spec
            .mapIndexed { index, sp ->
                val eventId = "EVT-${5000 + index}"
                EventRecord(
                    id = eventId,
                    title = sp.title,
                    venue = sp.venue,
                    category = sp.category,
                    capacity = sp.capacity,
                    actualAttendance = sp.actualAttendance,
                    status = sp.status,
                    dateMillis = now - sp.daysAgo * dayMs,
                    budgetedAmountMinor = sp.budgetedAmountMinor,
                    actualAmountMinor = sp.actualAmountMinor,
                    linkedExpenses = if (index == 0) listOf(expensePool[0], expensePool[1]) else emptyList(),
                )
            }.toMutableList()
    }

    private data class Spec(
        val title: String,
        val venue: String,
        val category: EventCategory,
        val capacity: Int,
        val actualAttendance: Int,
        val status: EventStatus,
        val daysAgo: Long,
        val budgetedAmountMinor: Long,
        val actualAmountMinor: Long,
    )
}
