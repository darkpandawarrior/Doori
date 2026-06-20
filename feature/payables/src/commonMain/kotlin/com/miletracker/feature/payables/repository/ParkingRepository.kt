package com.miletracker.feature.payables.repository

/** Whether a parking event records a vehicle arriving at, or departing from, the gate (PB.3). */
enum class ParkMode(val label: String) {
    IN("Park In"),
    OUT("Park Out"),
}

/** A create Park In / Park Out gate-event form payload (PB.3). */
data class ParkingDraft(
    val mode: ParkMode,
    val vehicleNumber: String,
    val driverName: String,
    val gate: String,
    val poReference: String,
    val remarks: String,
)

/**
 * Offline fake gate-parking store (PB.3) — persists submitted Park In / Park Out events in-memory and returns
 * a **rotating** [PayablesSubmissionResult] so the logged / approval / security-hold paths are all exercised
 * across repeated submits. No backend; mirrors the PB.1 [InvoiceRepository] / PB.2 [GinRepository] pattern.
 */
class ParkingRepository {
    private val submitted = mutableListOf<ParkingDraft>()
    private var counter = 0

    fun submit(draft: ParkingDraft): PayablesSubmissionResult {
        submitted += draft
        val prefix = if (draft.mode == ParkMode.IN) "PIN" else "POUT"
        val id = "$prefix-${3100 + submitted.size}"
        return when (counter++ % 3) {
            0 -> PayablesSubmissionResult.Submitted(id)
            1 -> PayablesSubmissionResult.NeedsApproval(id)
            else ->
                PayablesSubmissionResult.PolicyViolation(
                    listOf("Vehicle number not on the approved list", "Gate event needs security clearance"),
                )
        }
    }

    fun count(): Int = submitted.size
}
