package com.mileway.core.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class ExpenseSourceContextTest {
    @Test
    fun `None and Regular are distinct singletons`() {
        assertSame(ExpenseSourceContext.None, ExpenseSourceContext.None)
        assertNotEquals<ExpenseSourceContext>(ExpenseSourceContext.None, ExpenseSourceContext.Regular)
    }

    @Test
    fun `id-carrying variants are equal by value`() {
        assertEquals(ExpenseSourceContext.Edit("exp-1"), ExpenseSourceContext.Edit("exp-1"))
        assertNotEquals(ExpenseSourceContext.Edit("exp-1"), ExpenseSourceContext.Edit("exp-2"))

        assertEquals(ExpenseSourceContext.Trip("trip-1"), ExpenseSourceContext.Trip("trip-1"))

        assertEquals(
            ExpenseSourceContext.TripAdvance("trip-1", "adv-1"),
            ExpenseSourceContext.TripAdvance("trip-1", "adv-1"),
        )
        assertNotEquals(
            ExpenseSourceContext.TripAdvance("trip-1", "adv-1"),
            ExpenseSourceContext.TripAdvance("trip-1", "adv-2"),
        )

        assertEquals(ExpenseSourceContext.Event("evt-1"), ExpenseSourceContext.Event("evt-1"))

        assertEquals(
            ExpenseSourceContext.Card("card-1", "txn-1"),
            ExpenseSourceContext.Card("card-1", "txn-1"),
        )
        assertNotEquals(
            ExpenseSourceContext.Card("card-1", "txn-1"),
            ExpenseSourceContext.Card("card-1", "txn-2"),
        )

        assertEquals(ExpenseSourceContext.Advance("adv-1"), ExpenseSourceContext.Advance("adv-1"))

        assertEquals(
            ExpenseSourceContext.Message("clar-1", "https://example/att.jpg"),
            ExpenseSourceContext.Message("clar-1", "https://example/att.jpg"),
        )
    }

    @Test
    fun `Scanner wraps a ScannerPrefill by value equality`() {
        val prefill =
            ScannerPrefill(
                merchant = "Acme Fuel",
                amountText = "42.50",
                currency = "USD",
                dateEpochMs = 1_700_000_000_000L,
                category = "Fuel",
                overallConfidence = 0.87f,
                duplicateWarning = null,
            )

        assertEquals(ExpenseSourceContext.Scanner(prefill), ExpenseSourceContext.Scanner(prefill.copy()))
        assertNotEquals(
            ExpenseSourceContext.Scanner(prefill),
            ExpenseSourceContext.Scanner(prefill.copy(amountText = "99.00")),
        )
    }

    @Test
    fun `ScannerPrefill tolerates fully-absent extraction`() {
        val emptyPrefill =
            ScannerPrefill(
                merchant = null,
                amountText = null,
                currency = null,
                dateEpochMs = null,
                category = null,
                overallConfidence = 0f,
                duplicateWarning = null,
            )

        assertEquals(emptyPrefill, emptyPrefill.copy())
    }
}
