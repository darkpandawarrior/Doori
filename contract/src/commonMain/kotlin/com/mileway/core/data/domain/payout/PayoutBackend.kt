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
    override fun payout(journal: PendingPaymentJournal): PendingPaymentJournal = journal.copy(status = PaymentStatus.PAID)
}
