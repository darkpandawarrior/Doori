package com.mileway.core.data.claim

import com.mileway.core.data.dao.PendingPaymentJournalDao
import com.mileway.core.data.dao.ReportDao
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.payout.PaymentStatus
import com.mileway.core.data.domain.payout.PendingPaymentJournal
import com.mileway.core.data.domain.payout.SimulatedPayoutBackend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Durable report payouts. Deliberately accepts only the pure simulator, never a bank adapter. */
class ReportPayoutProcessor(
    private val reports: ReportRepository,
    private val reportDao: ReportDao,
    private val journals: PendingPaymentJournalDao,
) {
    private val simulator = SimulatedPayoutBackend()
    // ponytail: one local payout at a time; per-report locks if queue throughput matters.
    private val mutex = Mutex()

    suspend fun pay(reportId: String): Report = mutex.withLock {
        var report = requireNotNull(reports.get(reportId)) { "Report not found" }
        if (report.state == ReportLifecycleState.PAID) return@withLock report
        if (report.state == ReportLifecycleState.APPROVED) {
            report = reports.transition(reportId, ReportLifecycleEvent.RELEASE_FOR_PAYMENT)
        }
        require(report.state == ReportLifecycleState.APPROVED_FOR_PAYMENT) { "Report is not approved for payment" }
        // transition() has already COMMITTED the pending Room row before this call.
        val journal = requireNotNull(journals.getByReport(reportId).singleOrNull { it.id == payoutJournalId(reportId) })
        val receipt = simulator.payout(PendingPaymentJournal(
            reportId = journal.reportId,
            amountMinor = journal.amountMinor,
            currency = journal.currency,
            status = PaymentStatus.valueOf(journal.status),
            createdAtMillis = journal.createdAtMs,
        ))
        reports.completePayment(receipt)
    }

    /** Retry all approved reports after launch. One failure leaves its journal intact for a later retry. */
    suspend fun recover(): Map<String, String> {
        val failures = mutableMapOf<String, String>()
        for (report in reportDao.awaitingPayment()) {
            try {
                pay(report.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                failures[report.id] = failure.message ?: "Payout retry failed"
            }
        }
        return failures
    }
}
