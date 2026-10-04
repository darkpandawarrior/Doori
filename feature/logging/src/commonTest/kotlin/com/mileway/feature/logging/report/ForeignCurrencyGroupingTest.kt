package com.mileway.feature.logging.report

import com.mileway.core.data.domain.claim.Attendee
import com.mileway.core.data.domain.claim.FxRate
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForeignCurrencyGroupingTest {
    @Test
    fun foreignLineRetainsCurrencyAndUsesThePinWithAndWithoutDetails() {
        val record =
            ExpenseRecord(
                "fx",
                ExpenseCategory.FOOD,
                "Cafe",
                100.0,
                ExpenseStatus.PENDING,
                0,
                currencyCode = "USD",
                amountMinor = 10000,
                fxRate = FxRate(90.0, "USD", sourceDate = "2026-09-25"),
                fxRatePinnedAt = 123,
            )
        for (entry in listOf(record, record.copy(attendees = listOf(Attendee("Alex"))))) {
            val line = entry.toClaimLine()
            assertEquals("USD", line.currency)
            assertEquals(10000L, line.amountMinor)
            assertEquals(entry.fxRate, line.fxRate)
            assertEquals(123L, line.fxRatePinnedAt)
            assertTrue(expenseReportPolicy().evaluate(listOf(line), 0)[line.id].orEmpty().any { it.code == "EXPENSE_OVER_MAX" })
            val offline = entry.copy(fxRate = null, fxRatePinnedAt = null).toClaimLine()
            assertEquals("FX_POLICY_SKIPPED", expenseReportPolicy().evaluate(listOf(offline), 0)[offline.id]?.single()?.code)
        }
    }
}
