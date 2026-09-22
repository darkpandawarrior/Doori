package com.mileway.stub.utils

import java.util.Locale

private val CURRENCY_SYMBOLS =
    mapOf(
        "INR" to "₹",
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "JPY" to "¥",
        "CNY" to "¥",
        "AUD" to "A$",
        "CAD" to "C$",
        "SGD" to "S$",
        "AED" to "د.إ",
        "SAR" to "﷼",
        "MYR" to "RM",
    )

object FormattingUtils {
    fun getCurrencySymbol(currency: String): String = CURRENCY_SYMBOLS[currency.uppercase()] ?: "₹"

    fun formatAmount(amount: Double): String =
        try {
            String.format(Locale.getDefault(), "%,.0f", amount)
        } catch (ignored: IllegalArgumentException) {
            // String.format rejects a bad pattern with IllegalArgumentException; the raw number is the fallback.
            amount.toString()
        }

    fun formatAmountWithDecimals(
        amount: Double,
        decimals: Int = 2,
    ): String =
        try {
            String.format(Locale.getDefault(), "%,.${decimals}f", amount)
        } catch (ignored: IllegalArgumentException) {
            // Same fallback as above: a bad decimal count must not cost the caller its number.
            amount.toString()
        }

    fun formatAmountWithCurrency(
        amount: Double,
        currency: String,
    ): String = "${getCurrencySymbol(currency)} ${formatAmount(amount)}"
}
