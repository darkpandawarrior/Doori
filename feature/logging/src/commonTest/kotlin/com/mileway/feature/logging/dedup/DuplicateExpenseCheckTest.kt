package com.mileway.feature.logging.dedup

import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class DuplicateExpenseCheckTest {
    private val date = Instant.parse("2026-10-04T00:01:00Z").toEpochMilliseconds()
    private val record = ExpenseRecord("old", ExpenseCategory.FOOD, "Cafe  One", 10.01, ExpenseStatus.PENDING, date)

    @Test
    fun matchesMinorUnitsNormalizedMerchantAndAdjacentCalendarDays() {
        val dates = listOf("2026-10-03T00:00:00Z", "2026-10-04T23:59:00Z", "2026-10-05T23:59:00Z")
        dates.forEach { iso ->
            assertEquals(
                listOf(record),
                DuplicateExpenseCheck.matches(
                    10.01000001, " cafe\tONE ", Instant.parse(iso).toEpochMilliseconds(), "INR", listOf(record), timeZone = TimeZone.UTC,
                ),
            )
        }
    }

    @Test
    fun excludesDifferentMoneyCurrencyMerchantDatesDraftsAndSelf() {
        val nonMatches = listOf(
            record.copy(amountRupees = 10.02),
            record.copy(currencyCode = "USD"),
            record.copy(merchantName = "Other"),
            record.copy(dateMs = Instant.parse("2026-10-06T00:00:00Z").toEpochMilliseconds()),
            record.copy(status = ExpenseStatus.DRAFT),
        )
        assertTrue(DuplicateExpenseCheck.matches(10.01, "Cafe One", date, "INR", nonMatches, timeZone = TimeZone.UTC).isEmpty())
        assertTrue(DuplicateExpenseCheck.matches(10.01, "Cafe One", date, "INR", listOf(record), editingId = "old").isEmpty())
        assertTrue(DuplicateExpenseCheck.matches(Double.NaN, "Cafe One", date, "INR", listOf(record)).isEmpty())
    }
}
