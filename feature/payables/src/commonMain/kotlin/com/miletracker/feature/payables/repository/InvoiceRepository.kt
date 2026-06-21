package com.miletracker.feature.payables.repository

/** A create-invoice form payload (PB.1). */
data class InvoiceDraft(
    val invoiceNumber: String,
    val vendor: String,
    val amount: Double,
    val taxPercent: Double,
    val glCode: String,
)

/** Rotating submission outcome — exercises the success / approval / violation result paths (PB.1). */
sealed interface InvoiceSubmissionResult {
    data class Submitted(val id: String) : InvoiceSubmissionResult

    data class NeedsApproval(val id: String) : InvoiceSubmissionResult

    data class PolicyViolation(val messages: List<String>) : InvoiceSubmissionResult
}

/**
 * Offline fake invoice store (PB.1) — persists submitted drafts in-memory (survives navigation) and returns
 * a **rotating** [InvoiceSubmissionResult] so the success screen, the approval result, and the policy-
 * violation sheet are all exercised across repeated submits. No backend; mirrors the LogMilesSubmit pattern.
 */
class InvoiceRepository {
    private val submitted = mutableListOf<InvoiceDraft>()
    private var counter = 0

    fun submit(draft: InvoiceDraft): InvoiceSubmissionResult {
        submitted += draft
        val id = "INV-${7000 + submitted.size}"
        return when (counter++ % 3) {
            0 -> InvoiceSubmissionResult.Submitted(id)
            1 -> InvoiceSubmissionResult.NeedsApproval(id)
            else ->
                InvoiceSubmissionResult.PolicyViolation(
                    listOf("Amount exceeds the auto-approval limit", "GL code requires finance review"),
                )
        }
    }

    fun count(): Int = submitted.size
}
