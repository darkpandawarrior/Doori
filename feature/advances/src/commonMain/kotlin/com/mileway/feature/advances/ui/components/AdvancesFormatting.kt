package com.mileway.feature.advances.ui.components

import com.mileway.core.ui.text.monthName
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/*
 * PLAN_V35.P4: pure formatting helpers for the advance-wallet UI (commonMain-safe, no
 * java.text/android.* — mirrors feature/cards's formatMoney idiom).
 */

/** Thousands grouping puts a comma after every third digit, counting from the right. */
private const val ThousandsGroupSize = 3

/** Thousands-grouped rupee amount, e.g. 3200.0 -> "3,200". */
internal fun formatMoney(amount: Double): String {
    val whole = amount.toLong()
    return whole
        .toString()
        .reversed()
        .chunked(ThousandsGroupSize)
        .joinToString(",")
        .reversed()
}

/** "d MMM yyyy", e.g. "12 Oct 2026". */
internal fun formatDate(ms: Long): String {
    val dt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth} ${monthName(dt.monthNumber)} ${dt.year}"
}
