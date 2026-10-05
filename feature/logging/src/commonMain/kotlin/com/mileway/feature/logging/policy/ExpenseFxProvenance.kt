package com.mileway.feature.logging.policy

import com.mileway.feature.logging.model.ExpenseRecord

/** A saved foreign line shows a valid pin's provider date, or an explicit unresolved state. */
fun ExpenseRecord.fxProvenanceLabel(): String? {
    if (currencyCode == "INR") return null
    return fxRate?.takeIf { fxRatePinnedAt != null && it.isUsable(currencyCode, "INR") }?.description()
        ?: "No FX pin: reference unavailable/offline; amount checks skipped. Manual rates are approximate."
}
