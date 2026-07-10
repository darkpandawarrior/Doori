package com.mileway.feature.logging.currency

/**
 * P27.E.15: local, offline, static conversion table for the expense entry form's currency picker
 * (DiCE's "Amount Details" card had currency + live conversion). Live FX-rate backends are out of
 * scope for Mileway (PLAN_V27 §4) — rates here are fixed at rest and only ever back a rough
 * "≈ ₹X" preview in the UI, never the amount actually stored on [com.mileway.feature.logging
 * .model.ExpenseRecord.amountRupees].
 *
 * ponytail: hardcoded static table, upgrade path is a real FX-rate source once a backend phase
 * exists (see CLAUDE.md "The backend").
 */
object CurrencyConverter {
    /** 1 unit of the key currency, expressed in rupees. Approximate, fixed — no live FX. */
    private val ratesInRupees: Map<String, Double> =
        mapOf(
            "INR" to 1.0,
            "USD" to 83.0,
            "EUR" to 90.0,
            "GBP" to 105.0,
            "AED" to 22.6,
            "SGD" to 62.0,
        )

    /** Currency codes the entry form's picker offers, in display order. */
    val supportedCurrencies: List<String> = ratesInRupees.keys.toList()

    private val symbols: Map<String, String> =
        mapOf(
            "INR" to "₹",
            "USD" to "$",
            "EUR" to "€",
            "GBP" to "£",
            "AED" to "AED",
            "SGD" to "S$",
        )

    /** Display symbol for [code]; falls back to the code itself when unrecognized. */
    fun symbolFor(code: String): String = symbols[code] ?: code

    /**
     * Converts [amount] from currency [from] to currency [to] via the static [ratesInRupees]
     * table. An unrecognized code (e.g. a corrupt persisted draft) is treated as INR (rate 1.0)
     * rather than throwing, so a bad currency code never crashes the entry screen.
     */
    fun convert(
        amount: Double,
        from: String,
        to: String,
    ): Double {
        val fromRate = ratesInRupees[from] ?: 1.0
        val toRate = ratesInRupees[to] ?: 1.0
        return amount * fromRate / toRate
    }

    /** Converts [amount] in [from] to its rupee equivalent — the entry form's live-preview case. */
    fun toRupees(
        amount: Double,
        from: String,
    ): Double = convert(amount, from, "INR")
}
