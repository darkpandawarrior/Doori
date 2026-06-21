package com.miletracker.feature.logging.repository

import kotlin.time.Clock

/** A corporate-card *expense* transaction awaiting reconciliation (SP.3). */
data class CardExpenseTxn(
    val id: String,
    val status: String,
    val merchant: String,
    val amount: Double,
    val cardLast4: String,
    val dateMillis: Long,
    val category: String,
)

/** Reconciliation states (SP.3 tabs). */
enum class CardTxnStatus(val label: String) {
    UNRECONCILED("Unreconciled"),
    RECONCILED("Reconciled"),
    DISPUTED("Disputed"),
}

/**
 * Offline fake of corporate-card expense transactions (SP.3), distinct from the corporate-card detail txns
 * in `feature:cards`; these are the spend records that flow into expense reconciliation. Clock-injected.
 */
class CardsTxnHistoryRepository(private val clock: Clock = Clock.System) {
    private val dayMs = 86_400_000L

    private fun all(): List<CardExpenseTxn> {
        val now = clock.now().toEpochMilliseconds()
        val merchants = listOf("Uber", "Taj Hotels", "Amazon Business", "IndiGo", "Starbucks", "Croma", "BigBasket")
        val categories = listOf("Travel", "Accommodation", "Office Supplies", "Travel", "Food", "Office Supplies", "Food")
        val spec =
            listOf(
                CardTxnStatus.UNRECONCILED to 1_240.0,
                CardTxnStatus.UNRECONCILED to 8_900.0,
                CardTxnStatus.UNRECONCILED to 540.0,
                CardTxnStatus.RECONCILED to 3_600.0,
                CardTxnStatus.RECONCILED to 12_400.0,
                CardTxnStatus.DISPUTED to 2_150.0,
                CardTxnStatus.RECONCILED to 760.0,
            )
        return spec.mapIndexed { index, (status, amount) ->
            CardExpenseTxn(
                id = "CTX-${5000 + index}",
                status = status.label,
                merchant = merchants[index % merchants.size],
                amount = amount,
                cardLast4 = listOf("4821", "9930")[index % 2],
                dateMillis = now - (index + 1) * 2 * dayMs,
                category = categories[index % categories.size],
            )
        }
    }

    fun transactions(status: CardTxnStatus? = null): List<CardExpenseTxn> =
        all().filter { status == null || it.status == status.label }.sortedByDescending { it.dateMillis }
}
