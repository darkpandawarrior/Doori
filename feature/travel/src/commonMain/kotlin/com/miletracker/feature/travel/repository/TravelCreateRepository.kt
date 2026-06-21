package com.miletracker.feature.travel.repository

/**
 * Shared rotating submission outcome for every TR create flow (Trip / Flight / Bus / Hotel / MJP / Visa).
 * Mirrors the PB payables result shape so each travel create surface exercises the success / approval /
 * policy-violation paths through the shared `FormSubmissionScaffold`.
 */
sealed interface TravelSubmissionResult {
    data class Submitted(val id: String) : TravelSubmissionResult

    data class NeedsApproval(val id: String) : TravelSubmissionResult

    data class PolicyViolation(val messages: List<String>) : TravelSubmissionResult
}

/** TR.2 — a create trip-request form payload. */
data class TripDraft(
    val purpose: String,
    val fromCity: String,
    val toCity: String,
    val startDate: String,
    val endDate: String,
    val advanceRequired: Boolean,
)

/** TR.3 — an add-flight booking-request form payload. */
data class FlightDraft(
    val fromCity: String,
    val toCity: String,
    val travelDate: String,
    val preferredAirline: String,
    val cabinClass: String,
)

/** TR.4 — an add-bus booking-request form payload. */
data class BusDraft(
    val fromCity: String,
    val toCity: String,
    val travelDate: String,
    val operator: String,
    val seatPreference: String,
)

/** TR.5 — an add-hotel booking-request form payload. */
data class HotelDraft(
    val city: String,
    val checkInDate: String,
    val checkOutDate: String,
    val guests: Int,
    val roomPreference: String,
)

/** TR.6 — one leg of a multi-city journey plan. */
data class MjpLeg(
    val fromCity: String,
    val toCity: String,
    val travelDate: String,
)

/** TR.6 — a multi-city journey-plan (MJP) form payload. */
data class MjpDraft(
    val purpose: String,
    val legs: List<MjpLeg>,
)

/**
 * Offline fake travel-create store (TR.2+) — persists submitted drafts in-memory and returns a **rotating**
 * [TravelSubmissionResult] so the confirmed / approval / policy-violation paths are all exercised across
 * repeated submits, for every TR create flow. No backend; one shared rotating counter (demo theater). Mirrors
 * the PB `InvoiceRepository` / `GinRepository` pattern, consolidated to one repo for the six travel flows.
 */
class TravelCreateRepository {
    private val submittedCount = mutableMapOf<String, Int>()
    private var counter = 0

    /** Rotates the three outcome paths; [idPrefix]/[base] shape the demo reference id, [violations] the warning copy. */
    private fun rotate(
        idPrefix: String,
        base: Int,
        violations: List<String>,
    ): TravelSubmissionResult {
        val n = (submittedCount[idPrefix] ?: 0) + 1
        submittedCount[idPrefix] = n
        val id = "$idPrefix-${base + n}"
        return when (counter++ % 3) {
            0 -> TravelSubmissionResult.Submitted(id)
            1 -> TravelSubmissionResult.NeedsApproval(id)
            else -> TravelSubmissionResult.PolicyViolation(violations)
        }
    }

    fun submitTrip(draft: TripDraft): TravelSubmissionResult =
        rotate(
            "TRP",
            4400,
            listOf("Trip dates overlap an existing request", "Destination requires travel-desk approval"),
        )

    fun submitFlight(draft: FlightDraft): TravelSubmissionResult =
        rotate(
            "FLT",
            5100,
            listOf("Fare exceeds the cabin-class cap", "Business class needs grade-L4+ approval"),
        )

    fun submitBus(draft: BusDraft): TravelSubmissionResult =
        rotate(
            "BUS",
            6200,
            listOf("Operator not on the approved panel", "Sleeper class needs overnight-travel approval"),
        )

    fun submitHotel(draft: HotelDraft): TravelSubmissionResult =
        rotate(
            "HTL",
            7300,
            listOf("Nightly rate exceeds the city tariff cap", "Stay over 3 nights needs manager approval"),
        )

    fun submitMjp(draft: MjpDraft): TravelSubmissionResult =
        rotate(
            "MJP",
            8400,
            listOf("A leg has overlapping dates", "Multi-city plans over 4 legs need travel-desk approval"),
        )
}
