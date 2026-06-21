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
}
