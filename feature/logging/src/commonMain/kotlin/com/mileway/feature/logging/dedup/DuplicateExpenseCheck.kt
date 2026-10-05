package com.mileway.feature.logging.dedup

import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Instant

private const val MinorPerUnit = 100.0
private val Whitespace = Regex("\\s+")

/** Save-time similarity check; dates are calendar dates in [timeZone], not elapsed 24-hour windows. */
object DuplicateExpenseCheck {
    fun matches(
        amount: Double,
        merchant: String,
        dateMs: Long,
        currency: String,
        records: List<ExpenseRecord>,
        editingId: String? = null,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        amountMinor: Long? = null,
    ): List<ExpenseRecord> {
        val minor = amountMinor ?: minorUnits(amount) ?: return emptyList()
        if (minor <= 0) return emptyList()
        val normalizedMerchant = normalize(merchant)
        if (normalizedMerchant.isEmpty()) return emptyList()
        val day =
            Instant
                .fromEpochMilliseconds(dateMs)
                .toLocalDateTime(timeZone)
                .date
                .toEpochDays()
        return records.filter { record ->
            val recordDay =
                Instant
                    .fromEpochMilliseconds(record.dateMs)
                    .toLocalDateTime(timeZone)
                    .date
                    .toEpochDays()
            record.id != editingId &&
                record.status != ExpenseStatus.DRAFT &&
                record.currencyCode.equals(currency, ignoreCase = true) &&
                (record.amountMinor ?: minorUnits(record.amountRupees)) == minor &&
                normalize(record.merchantName) == normalizedMerchant &&
                abs(recordDay.toLong() - day.toLong()) <= 1L
        }
    }

    private fun normalize(merchant: String): String = merchant.trim().lowercase().replace(Whitespace, " ")

    private fun minorUnits(amount: Double): Long? {
        val scaled = amount * MinorPerUnit
        return if (scaled.isFinite() && scaled > 0 && scaled < Long.MAX_VALUE.toDouble()) scaled.roundToLong() else null
    }
}
