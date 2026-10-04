package com.mileway.core.data.claim

import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ClaimLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.model.db.ApprovalStepEntity
import com.mileway.core.data.model.db.PeriodLockEntity
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

const val FINANCE_ROLE = "FINANCE"
const val MANAGER_ROLE = "manager"

/** Persisted review metadata stays outside the unchanged report wire contract. */
data class ApprovalReview(
    val report: Report,
    val actions: List<ApprovalStepEntity> = emptyList(),
    val submittedAtMs: Long? = null,
    val accountingPeriodKey: String? = null,
    val blockedLineIds: Set<String> = emptySet(),
) {
    val nextRole: String
        get() =
            if (actions.lastOrNull { it.claimLineId == null }?.let { it.role == MANAGER_ROLE && it.action == ApprovalAction.APPROVE.name } == true) {
                FINANCE_ROLE
            } else {
                MANAGER_ROLE
            }

    val rejectedLineIds: Set<String>
        get() =
            actions.filter { it.claimLineId != null }.groupBy { requireNotNull(it.claimLineId) }
                .filterValues { it.last().action == ApprovalAction.REJECT.name }.keys

    val payableLines: List<ClaimLine> get() = report.lines.filterNot { it.id in rejectedLineIds }
    val canBulkApprove: Boolean get() = payableLines.isNotEmpty() && payableLines.none { it.id in blockedLineIds }
}

/** Unknown policy codes fail closed; only the engine's explicit soft warnings can pass bulk review. */
fun ClaimLine.hasHardViolation(): Boolean = policyFlags.any { isHardPolicyCode(it) }

internal fun isHardPolicyCode(code: String): Boolean = code !in setOf("RECEIPT_RECOMMENDED", "MILEAGE_RATE_CAPPED")

/** Uses UTC accounting months and never alters the original submission instant. */
internal suspend fun nextOpenPeriod(
    submittedAtMs: Long,
    lock: suspend (String) -> PeriodLockEntity?,
): String {
    val date = Instant.fromEpochMilliseconds(submittedAtMs).toLocalDateTime(TimeZone.UTC).date
    var year = date.year
    var month = date.monthNumber
    while (true) {
        val key = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}"
        if (lock(key) == null) return key
        if (month == MONTHS_PER_YEAR) {
            year++
            month = 1
        } else {
            month++
        }
    }
}

private const val MONTHS_PER_YEAR = 12
