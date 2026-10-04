package com.mileway.core.forms

import com.mileway.core.data.domain.policy.PolicyEngine

/** Parent money and dated policy supplied by the host, never editable inside a child field. */
data class ExpenseFieldContext(
    val receiptAmountMinor: Long,
    val currencyCode: String = "INR",
    val cardMatchedAmountMinor: Long? = null,
    val policy: PolicyEngine? = null,
    val submittedAtMillis: Long = 0,
) {
    val anchorAmountMinor: Long get() = cardMatchedAmountMinor ?: receiptAmountMinor
}

/** Exact two-decimal input conversion. Invalid/incomplete text stays in the form until corrected. */
fun parseMinorAmount(text: String): Long? {
    val parts = text.trim().split('.')
    if (parts.size !in 1..2 || parts[0].isEmpty() || parts[0].any { !it.isDigit() }) return null
    val fraction = parts.getOrNull(1).orEmpty()
    if (fraction.length > 2 || fraction.any { !it.isDigit() }) return null
    val whole = parts[0].toLongOrNull() ?: return null
    val cents = fraction.padEnd(2, '0').toLongOrNull() ?: return null
    if (whole > (Long.MAX_VALUE - cents) / MinorPerUnit) return null
    return whole * MinorPerUnit + cents
}

internal const val MinorPerUnit = 100L

/** Checked nonnegative sum, shared by child-line reconciliation and split validation. */
internal fun checkedMinorTotal(amounts: List<Long>): Long? {
    var total = 0L
    for (amount in amounts) {
        if (amount < 0 || total > Long.MAX_VALUE - amount) return null
        total += amount
    }
    return total
}
