package com.mileway.feature.events.model

/**
 * Lifecycle status of an event (EV). [PENDING_APPROVAL] (V29 P29.E.5) turns the old rotating-outcome
 * create-event gimmick into a real navigable workflow: a `NeedsApproval` submission now persists a
 * row at this status, and the event-detail screen exposes a mock approve/reject action that flips it
 * to [PUBLISHED]/[CANCELLED].
 */
enum class EventStatus(val label: String) {
    DRAFT("Draft"),
    PENDING_APPROVAL("Pending approval"),
    PUBLISHED("Published"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed"),
}

/** V29 P29.E.4: fixed category taxonomy, replaces the old free-text `category: String` field. */
enum class EventCategory(val label: String) {
    ALL_HANDS("All-hands"),
    TECH("Tech"),
    CULTURE("Culture"),
    OFFSITE("Offsite"),
    WORKSHOP("Workshop"),
    OTHER("Other"),
}

/** A local expense linked to an event (V29 P29.E.1/E.8). No real voucher system backs this yet — seeded/local only. */
data class LinkedExpense(
    val id: String,
    val description: String,
    val amountMinor: Long,
    val dateMillis: Long,
)

/** One row in the events history (EV). */
data class EventRecord(
    val id: String,
    val title: String,
    val venue: String,
    val category: EventCategory,
    /** Planned attendance (V29 P29.E.2). */
    val capacity: Int,
    /** Actual attendance so far/at close, compared against [capacity] for the variance chip. */
    val actualAttendance: Int,
    val status: EventStatus,
    val dateMillis: Long,
    /** Budgeted cost in minor currency units (paise), V29 P29.E.3. */
    val budgetedAmountMinor: Long = 0L,
    /** Actual cost incurred so far, in minor currency units. */
    val actualAmountMinor: Long = 0L,
    val linkedExpenses: List<LinkedExpense> = emptyList(),
) {
    /** DiCE-parity `attendees` alias kept for the history card's existing display, actual head-count. */
    val attendees: Int get() = actualAttendance

    /** Delete (P29.E.7) is only allowed while the event hasn't locked into an approval/completion state. */
    val isDeletable: Boolean get() = status == EventStatus.DRAFT || status == EventStatus.PUBLISHED
}
