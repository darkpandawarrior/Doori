package com.mileway.feature.advances.model

/*
 * PLAN_V35.P3: advance-wallet data layer (commonMain, pure Kotlin). No android.* / java.* imports.
 * Two products: Petty Advance cards (cash-kit style, reconciled against a bucketed txn pipeline)
 * and QR cards (scan-to-pay, optionally transferable). Both share a request/approval lifecycle.
 */

/** Q.2-style reconciliation pipeline for a petty card's pending transactions. */
enum class ReconBucket {
    VOUCHER_NOT_CREATED,
    MANAGER_APPROVAL_PENDING,
    MANAGER_APPROVED,
    FINANCE_APPROVED,
    TRANSACTION_DECLINED,
}

data class PettyCard(
    val id: Long,
    val kitNo: String,
    val amount: Double,
    val balance: Double,
    val currency: String = "INR",
    val createdAtMs: Long,
    val dueOnMs: Long,
    val description: String,
    val title: String,
    val type: String,
    val colorSeed: String,
    val txnPendingAmount: Double,
    val txnAmountDistribution: Map<ReconBucket, Double> = emptyMap(),
) {
    val totalBalance: Double get() = balance + txnPendingAmount
}

data class QrCard(
    val id: Long,
    val qrId: String,
    val title: String,
    val description: String,
    val balance: Double,
    val cardLimit: Double,
    val total: Double,
    val validUntilMs: Long,
    val colorSeed: String,
    val currency: String = "INR",
    val isTransfer: Boolean = false,
)

data class AdvanceType(
    val id: Long,
    val title: String,
    val colorSeed: String,
)

/** Card-face health badge, derived from remaining balance against the card's total. */
enum class CardHealth { ACTIVE, LOW_BALANCE, CRITICAL }

/**
 * Active: balance >= 50% of total. LowBalance: >= 20%. Critical: below 20% (also covers a
 * zero/negative total, which can't sustain any spend).
 */
fun cardHealth(
    balance: Double,
    total: Double,
): CardHealth {
    if (total <= 0.0) return CardHealth.CRITICAL
    val ratio = balance / total
    return when {
        ratio >= 0.5 -> CardHealth.ACTIVE
        ratio >= 0.2 -> CardHealth.LOW_BALANCE
        else -> CardHealth.CRITICAL
    }
}

enum class AdvanceSection { PETTY, QR }

enum class AdvanceRequestStatus {
    PENDING,
    APPROVAL,
    APPROVED,
    DECLINED,
    ;

    /**
     * Reference-app status label: PENDING/APPROVAL map to a finance/manager-pending copy,
     * DECLINED forks on whether an admin (finance) or a manager declined it, everything else
     * (APPROVED, and DECLINED-by-manager) falls back to the raw enum name.
     */
    fun displayStatus(declinedByAdmin: Boolean = false): String =
        when {
            this == PENDING -> "FINANCE PENDING"
            this == APPROVAL -> "MANAGER PENDING"
            this == DECLINED && declinedByAdmin -> "FINANCE DECLINED"
            else -> name
        }
}

enum class GranterStatus { PENDING, APPROVED, REJECTED }

data class Granter(
    val name: String,
    val role: String,
    val status: GranterStatus,
)

data class AdvanceRequest(
    val id: Long,
    val title: String,
    val description: String,
    val amount: Double,
    val currency: String = "INR",
    val type: String,
    val section: AdvanceSection,
    val status: AdvanceRequestStatus,
    val createdAtMs: Long,
    val granters: List<Granter> = emptyList(),
)

data class AdvanceTransaction(
    val id: Long,
    val title: String,
    val amount: Double,
    val currency: String = "INR",
    val dateMs: Long,
    val voucherCreated: Boolean,
    val status: ReconBucket,
)

/** Result of a successful petty/QR request submission — the reference app's `permissionId`. */
data class SubmittedRequest(val permissionId: Long)
