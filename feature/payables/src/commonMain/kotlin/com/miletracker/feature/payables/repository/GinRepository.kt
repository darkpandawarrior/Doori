package com.miletracker.feature.payables.repository

/**
 * Shared rotating submission outcome for the payables create flows that follow PB.1 (GIN, Park In/Out). Mirrors
 * the PB.1 `InvoiceSubmissionResult` shape so every payables create surface exercises the success / approval /
 * policy-violation paths through the shared `FormSubmissionScaffold`.
 */
sealed interface PayablesSubmissionResult {
    data class Submitted(val id: String) : PayablesSubmissionResult

    data class NeedsApproval(val id: String) : PayablesSubmissionResult

    data class PolicyViolation(val messages: List<String>) : PayablesSubmissionResult
}

/** A create-GIN (Goods Inward Note) form payload (PB.2). */
data class GinDraft(
    val ginNumber: String,
    val poReference: String,
    val vendor: String,
    val warehouse: String,
    val receivedQty: Int,
    val remarks: String,
)

/**
 * Offline fake GIN store (PB.2), persists submitted drafts in-memory and returns a **rotating**
 * [PayablesSubmissionResult] so the receipt-acknowledged, approval, and QC-hold paths are all exercised across
 * repeated submits. No backend; mirrors the PB.1 [InvoiceRepository] pattern.
 */
class GinRepository {
    private val submitted = mutableListOf<GinDraft>()
    private var counter = 0

    fun submit(draft: GinDraft): PayablesSubmissionResult {
        submitted += draft
        val id = "GIN-${5200 + submitted.size}"
        return when (counter++ % 3) {
            0 -> PayablesSubmissionResult.Submitted(id)
            1 -> PayablesSubmissionResult.NeedsApproval(id)
            else ->
                PayablesSubmissionResult.PolicyViolation(
                    listOf("Received quantity exceeds the PO quantity", "Goods receipt requires QC clearance"),
                )
        }
    }

    fun count(): Int = submitted.size
}
