package com.mileway.feature.cards.match

import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.amountInCurrencyMinor
import com.mileway.core.data.domain.claim.isFxSourceDate
import com.mileway.feature.cards.import.MaxStatementFieldLength
import com.mileway.feature.cards.import.StatementRow
import kotlin.time.Instant

/** Pure Kotlin matching; amounts never cross currency without the existing saved conversion pin. */
class CardMatcher(
    private val amountToleranceMinor: Long = 100,
    private val dateWindowDays: Long = 3,
    private val minimumMerchantSimilarity: Double = 0.7,
) {
    init {
        require(amountToleranceMinor >= 0 && dateWindowDays >= 0 && minimumMerchantSimilarity in 0.0..1.0)
    }

    data class Result(
        val line: ExpenseLine? = null,
        val reason: String? = null,
    )

    private data class Scored(
        val line: ExpenseLine,
        val amountDelta: Long,
        val days: Long,
        val merchant: Double,
    )

    fun match(
        row: StatementRow,
        candidates: List<ExpenseLine>,
    ): Result {
        require(row.amountMinor > 0 && isFxSourceDate(row.postingDate))
        val available = candidates.filter { it.cardMatchId == null && it.amountMinor > 0 }
        val scored = available.mapNotNull { line -> score(row, line) }
        val best = scored.minWithOrNull(compareBy<Scored> { it.amountDelta }.thenBy { it.days }.thenByDescending { it.merchant }.thenBy { it.line.id })
        val reason =
            when {
                best != null -> null
                available.any {
                    it.currency != row.currency && comparableAmounts(row, it) == null
                } -> "Foreign amount check unavailable: no usable FX pin or original foreign amount"
                available.any { it.incurredOn == null } -> "Claim capture date unavailable"
                else -> "No amount, date and merchant match within tolerance"
            }
        return Result(best?.line, reason)
    }

    private fun score(
        row: StatementRow,
        line: ExpenseLine,
    ): Scored? {
        if (line.merchant.length > MaxStatementFieldLength || row.merchant.length > MaxStatementFieldLength) return null
        val amounts = comparableAmounts(row, line) ?: return null
        val delta = if (amounts.first >= amounts.second) amounts.first - amounts.second else amounts.second - amounts.first
        if (delta > amountToleranceMinor) return null
        val date = line.incurredOn?.takeIf(::isFxSourceDate) ?: return null
        val days = kotlin.math.abs(epochDay(date) - epochDay(row.postingDate))
        if (days > dateWindowDays) return null
        val similarity = merchantSimilarity(line.merchant, row.merchant)
        return if (similarity >= minimumMerchantSimilarity) Scored(line, delta, days, similarity) else null
    }

    private fun comparableAmounts(
        row: StatementRow,
        line: ExpenseLine,
    ): Pair<Long, Long>? =
        when {
            row.currency == line.currency -> line.amountMinor to row.amountMinor
            row.foreignCurrency == line.currency && row.foreignAmountMinor != null && row.foreignAmountMinor > 0 -> line.amountMinor to row.foreignAmountMinor
            else -> line.amountInCurrencyMinor(row.currency)?.let { it to row.amountMinor }
        }
}

private const val MillisPerDay = 86_400_000L

private fun epochDay(date: String): Long = Instant.parse("${date}T00:00:00Z").toEpochMilliseconds() / MillisPerDay

/** Normalized edit distance tolerates typos while rejecting unrelated merchants. */
internal fun merchantSimilarity(
    first: String,
    second: String,
): Double {
    val a = first.lowercase().filter(Char::isLetterOrDigit)
    val b = second.lowercase().filter(Char::isLetterOrDigit)
    if (a.isEmpty() || b.isEmpty()) return 0.0
    var previous = IntArray(b.length + 1) { it }
    for (i in a.indices) {
        val current = IntArray(b.length + 1)
        current[0] = i + 1
        for (j in b.indices) current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + if (a[i] == b[j]) 0 else 1)
        previous = current
    }
    return 1.0 - previous[b.length].toDouble() / maxOf(a.length, b.length)
}
