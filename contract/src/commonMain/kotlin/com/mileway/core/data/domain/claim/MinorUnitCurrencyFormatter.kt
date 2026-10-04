package com.mileway.core.data.domain.claim

import kotlin.math.abs

private const val MinorUnitsPerMajor = 100L
private const val CurrencyFractionDigits = 2
private const val CurrencyGroupDigits = 3

/** Formats two-decimal claim currencies with grouping, without floating-point rounding. */
fun formatMinorCurrency(
    amountMinor: Long,
    currency: String,
): String {
    val symbol =
        when (currency) {
            "INR" -> "₹"
            "USD" -> "$"
            "GBP" -> "£"
            "EUR" -> "€"
            else -> currency
        }
    // Divide before taking abs so Long.MIN_VALUE is also representable.
    val whole =
        abs(amountMinor / MinorUnitsPerMajor)
            .toString()
            .reversed()
            .chunked(CurrencyGroupDigits)
            .joinToString(",")
            .reversed()
    val fraction = abs(amountMinor % MinorUnitsPerMajor).toString().padStart(CurrencyFractionDigits, '0')
    val sign = if (amountMinor < 0) "-" else ""
    return "$symbol $sign$whole.$fraction"
}
