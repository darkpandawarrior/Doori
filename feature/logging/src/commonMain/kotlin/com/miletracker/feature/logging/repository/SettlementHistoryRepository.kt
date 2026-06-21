package com.miletracker.feature.logging.repository

import kotlin.time.Clock

/** A settlement (reimbursement batch) record for SP.2. */
data class SettlementRecord(
    val id: String,
    val status: String,
    val amount: Double,
    val method: String,
    val periodLabel: String,
    val settledOnMillis: Long,
    val itemCount: Int,
)

/** Settlement lifecycle states (SP.2 tabs). */
enum class SettlementStatus(val label: String) {
    PENDING("Pending"),
    PROCESSING("Processing"),
    SETTLED("Settled"),
}

/**
 * Offline fake settlement store (SP.2) — a deterministic spread across [SettlementStatus]es, Clock-injected.
 */
class SettlementHistoryRepository(private val clock: Clock = Clock.System) {
    private val dayMs = 86_400_000L
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")

    private fun all(): List<SettlementRecord> {
        val now = clock.now().toEpochMilliseconds()
        val spec =
            listOf(
                Triple(SettlementStatus.PENDING, 18_400.0, 2),
                Triple(SettlementStatus.PENDING, 6_120.0, 1),
                Triple(SettlementStatus.PROCESSING, 24_900.0, 3),
                Triple(SettlementStatus.SETTLED, 31_500.0, 5),
                Triple(SettlementStatus.SETTLED, 9_750.0, 2),
                Triple(SettlementStatus.SETTLED, 42_300.0, 7),
            )
        return spec.mapIndexed { index, (status, amount, items) ->
            SettlementRecord(
                id = "STL-${2200 + index}",
                status = status.label,
                amount = amount,
                method = listOf("Bank Transfer", "Payroll", "Wallet")[index % 3],
                periodLabel = "${months[index % months.size]} 2026",
                settledOnMillis = now - (index + 1) * 4 * dayMs,
                itemCount = items,
            )
        }
    }

    fun settlements(status: SettlementStatus? = null): List<SettlementRecord> =
        all().filter { status == null || it.status == status.label }.sortedByDescending { it.settledOnMillis }
}
