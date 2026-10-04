package com.mileway.core.data.domain.claim

import kotlinx.serialization.Serializable
import kotlin.math.roundToLong
import kotlin.time.Instant

/** Provenance of a saved FX rate. Manual rates are always approximate. */
@Serializable
enum class FxRateSource { ECB_REFERENCE, CARD_MATCHED, MANUAL_APPROXIMATE }

/** Major units of [quoteCurrency] per unit of [baseCurrency]; [sourceDate] is the provider's date. */
@Serializable
data class FxRate(
    val rate: Double,
    val baseCurrency: String,
    val quoteCurrency: String = "INR",
    val sourceDate: String? = null,
    val source: FxRateSource = FxRateSource.ECB_REFERENCE,
) {
    /** User-facing provenance; manual estimates never claim a provider observation date. */
    fun description(): String {
        val label =
            when (source) {
                FxRateSource.ECB_REFERENCE -> "ECB reference (Frankfurter)"
                FxRateSource.CARD_MATCHED -> "Card matched"
                FxRateSource.MANUAL_APPROXIMATE -> "Manual (approximate)"
            }
        return "$label: 1 $baseCurrency = $rate $quoteCurrency; rate date ${sourceDate ?: "unavailable (manual)"}"
    }

    /** Invalid or undated reference rates must never enter policy money math. */
    fun isUsable(
        base: String,
        quote: String,
    ): Boolean =
        baseCurrency == base &&
            quoteCurrency == quote &&
            rate.isFinite() &&
            rate > 0 &&
            (source == FxRateSource.MANUAL_APPROXIMATE || sourceDate?.let(::isFxSourceDate) == true)
}

/** Converts only the currencies offered by capture (all use two decimal minor units). */
fun ExpenseLine.amountInCurrencyMinor(target: String): Long? {
    if (currency == target) return amountMinor
    val pinned = fxRate ?: return null
    if (fxRatePinnedAt == null || !pinned.isUsable(currency, target)) return null
    // Unknown minor-unit scales need an ISO currency table before conversion can be safe.
    val supported = setOf("INR", "USD", "EUR", "GBP", "AED", "SGD")
    if (currency !in supported || target !in supported) return null
    val converted = amountMinor.toDouble() * pinned.rate
    return if (converted.isFinite() && converted >= 0 && converted < Long.MAX_VALUE.toDouble()) converted.roundToLong() else null
}

/** Strict ISO calendar date validation using the existing Kotlin time API. */
fun isFxSourceDate(date: String): Boolean = runCatching { Instant.parse("${date}T00:00:00Z").toString().substringBefore('T') == date }.getOrDefault(false)
