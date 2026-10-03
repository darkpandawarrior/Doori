package com.mileway.core.data.domain.payout

/**
 * Moves a [PendingPaymentJournal] from pending to paid. [SimulatedPayoutBackend] is the only
 * implementation Doori ships — no real bank/UPI rail exists or is planned behind this interface;
 * `useRealBackend` stays compile-time `false` regardless of which [PayoutBackend] is bound.
 */
interface PayoutBackend {
    fun payout(journal: PendingPaymentJournal): PendingPaymentJournal
}

/**
 * In-process payout simulator: always succeeds, synchronously, no network call. This is the
 * *only* [PayoutBackend] in this codebase — real money movement is refused by construction, not by
 * a runtime flag.
 */
class SimulatedPayoutBackend : PayoutBackend {
    override fun payout(journal: PendingPaymentJournal): PendingPaymentJournal {
        require(journal.reportId.isNotBlank()) { "Report id is required" }
        require(journal.amountMinor > 0) { "Payout must be positive" }
        require(journal.currency.matches(Regex("[A-Z]{3}"))) { "Currency must be an ISO code" }
        // A pure simulator has no external side effect: replay returns the same report-keyed
        // receipt even after process death. Never replace this with a real rail.
        return journal.copy(status = PaymentStatus.PAID)
    }
}

/** Feature-boundary seam: approvals requests payouts through the existing payments repository. */
fun interface ReportPaymentRunner {
    suspend fun pay(reportId: String): com.mileway.core.data.domain.claim.Report
}
