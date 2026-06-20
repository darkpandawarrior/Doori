package com.miletracker.feature.events.repository

import com.miletracker.feature.events.model.EventRecord
import com.miletracker.feature.events.model.EventStatus
import kotlin.time.Clock

/** A create-event form payload (EV). */
data class EventDraft(
    val title: String,
    val venue: String,
    val date: String,
    val capacity: Int,
    val category: String,
)

/** Rotating submission outcome for the create-event flow (EV). */
sealed interface EventResult {
    data class Submitted(val id: String) : EventResult

    data class NeedsApproval(val id: String) : EventResult

    data class PolicyViolation(val messages: List<String>) : EventResult
}

/**
 * Offline fake events store (EV) — seeds a deterministic history (Clock-injected, no `Math.random`) and returns
 * a **rotating** [EventResult] (published / approval / policy-violation) across repeated submits. Mirrors the
 * PB/TR/PM fake-repo pattern.
 */
class EventsRepository(private val clock: Clock = Clock.System) {
    private val dayMs = 86_400_000L
    private val submitted = mutableListOf<EventDraft>()
    private var counter = 0

    fun submit(draft: EventDraft): EventResult {
        submitted += draft
        val id = "EVT-${3300 + submitted.size}"
        return when (counter++ % 3) {
            0 -> EventResult.Submitted(id)
            1 -> EventResult.NeedsApproval(id)
            else ->
                EventResult.PolicyViolation(
                    listOf("Venue capacity exceeds the booking limit", "Budget needs finance approval"),
                )
        }
    }

    fun count(): Int = submitted.size

    private fun all(): List<EventRecord> {
        val now = clock.now().toEpochMilliseconds()
        val spec =
            listOf(
                Spec("Q3 Town Hall", "Auditorium A", "All-hands", 320, EventStatus.PUBLISHED, 2L),
                Spec("Android Guild Meetup", "Pune Office", "Tech", 45, EventStatus.COMPLETED, 12L),
                Spec("Diwali Celebration", "Terrace", "Culture", 180, EventStatus.DRAFT, 5L),
                Spec("Sales Kickoff 2026", "Grand Hyatt", "Offsite", 95, EventStatus.CANCELLED, 20L),
                Spec("Design Sprint", "BKC Hub", "Workshop", 24, EventStatus.PUBLISHED, 1L),
            )
        return spec.mapIndexed { index, sp ->
            EventRecord(
                id = "EVT-${5000 + index}",
                title = sp.title,
                venue = sp.venue,
                category = sp.category,
                attendees = sp.attendees,
                status = sp.status,
                dateMillis = now - sp.daysAgo * dayMs,
            )
        }
    }

    /** All events, or just those in [status] when non-null, newest first. */
    fun events(status: EventStatus? = null): List<EventRecord> = all().filter { status == null || it.status == status }.sortedByDescending { it.dateMillis }

    private data class Spec(
        val title: String,
        val venue: String,
        val category: String,
        val attendees: Int,
        val status: EventStatus,
        val daysAgo: Long,
    )
}
