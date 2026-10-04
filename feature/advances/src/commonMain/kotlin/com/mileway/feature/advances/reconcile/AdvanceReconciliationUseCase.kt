package com.mileway.feature.advances.reconcile

import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.amountInCurrencyMinor
import com.mileway.feature.advances.data.AdvancesRepository
import kotlinx.coroutines.flow.first

/** An excluded line remains visible with a reason; its amount never enters either total. */
data class UnreconciledLine(
    val lineId: String,
    val reason: String,
)

/** Positive net is owed to the employee; negative net is owed back. All totals use minor units. */
data class AdvanceReconciliation(
    val currency: String,
    val spendMinor: Long,
    val appliedMinor: Long,
    val netMinor: Long,
    val excluded: List<UnreconciledLine>,
)

/**
 * Reads funded advances from the existing repository, without a second ledger or any write.
 * Report AdvanceLines select card IDs; their copied amounts are not authoritative.
 * Other persisted reports reserve their advance IDs, including drafts, to avoid reuse.
 * This calculation does not implement an atomic cross-report reservation or settlement.
 */
class AdvanceReconciliationUseCase(
    private val advances: AdvancesRepository,
) {
    /** Observe the repository's transient minor-unit view for report review. */
    fun advanceLines() = advances.reconciliationLines()

    /** [otherReports] must contain the owner's persisted reports, not just the current report. */
    suspend operator fun invoke(
        report: Report,
        otherReports: List<Report>,
        currency: String = report.currency(),
    ): AdvanceReconciliation = reconcile(report, advanceLines().first(), otherReports, currency)

    /** Recomputes from a repository emission; foreign expenses require their saved FX pin. */
    fun reconcile(
        report: Report,
        available: List<AdvanceLine>,
        otherReports: List<Report>,
        currency: String = report.currency(),
    ): AdvanceReconciliation {
        val excluded = mutableListOf<UnreconciledLine>()
        val reserved = otherReports.filter { it.id != report.id }.flatMap { it.lines.filterIsInstance<AdvanceLine>() }.map { it.advanceId }.toSet()
        val byId = available.groupBy { it.advanceId }
        val seen = mutableSetOf<String>()
        var spend = 0L
        var applied = 0L
        for (line in report.lines) {
            if (line is AdvanceLine) {
                val source = byId[line.advanceId]?.singleOrNull()
                val reason =
                    when {
                        !seen.add(line.advanceId) -> "Advance appears more than once on this report"
                        line.advanceId in reserved -> "Advance is already applied to another report"
                        source == null -> "Advance is unavailable or ambiguous in the repository"
                        source.reconciled -> "Advance is already reconciled"
                        source.currency != currency -> "Advance currency has no usable FX pin to $currency"
                        source.amountMinor < 0 -> "Advance amount must be nonnegative"
                        else -> null
                    }
                if (reason != null) excluded += UnreconciledLine(line.id, reason) else applied = addMinor(applied, requireNotNull(source).amountMinor)
            } else {
                val amount = if (line is ExpenseLine) line.amountInCurrencyMinor(currency) else line.amountMinor.takeIf { line.currency == currency }
                when {
                    line.amountMinor < 0 -> excluded += UnreconciledLine(line.id, "Claim amount must be nonnegative")
                    amount == null -> excluded += UnreconciledLine(line.id, "Claim currency has no usable FX pin to $currency")
                    else -> spend = addMinor(spend, amount)
                }
            }
        }
        return AdvanceReconciliation(currency, spend, applied, spend - applied, excluded)
    }
}

private fun addMinor(
    total: Long,
    amount: Long,
): Long {
    require(amount >= 0 && total <= Long.MAX_VALUE - amount) { "Reconciliation total exceeds supported minor units" }
    return total + amount
}
