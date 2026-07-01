package com.mileway.feature.events.model

/** Lifecycle status of an event (EV). */
enum class EventStatus(val label: String) {
    DRAFT("Draft"),
    PUBLISHED("Published"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed"),
}

/** One row in the events history (EV). */
data class EventRecord(
    val id: String,
    val title: String,
    val venue: String,
    val category: String,
    val attendees: Int,
    val status: EventStatus,
    val dateMillis: Long,
)
