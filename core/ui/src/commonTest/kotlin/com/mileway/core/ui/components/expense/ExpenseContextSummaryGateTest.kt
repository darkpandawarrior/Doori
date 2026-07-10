package com.mileway.core.ui.components.expense

import com.mileway.core.common.UiText
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.data.model.ScannerPrefill
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ExpenseContextSummaryGateTest {
    @Test
    fun `None and Regular have no summary`() {
        assertFalse(ExpenseSourceContext.None.hasSummary())
        assertFalse(ExpenseSourceContext.Regular.hasSummary())
    }

    @Test
    fun `every other variant has a summary`() {
        val prefill =
            ScannerPrefill(
                merchant = null,
                amountText = null,
                currency = null,
                dateEpochMs = null,
                category = null,
                overallConfidence = 0f,
                duplicateWarning = null,
            )
        val others =
            listOf(
                ExpenseSourceContext.Edit("exp-1"),
                ExpenseSourceContext.Trip("trip-1"),
                ExpenseSourceContext.TripAdvance("trip-1", "adv-1"),
                ExpenseSourceContext.Event("evt-1"),
                ExpenseSourceContext.Card("card-1", "txn-1"),
                ExpenseSourceContext.Advance("adv-1"),
                ExpenseSourceContext.Message("clar-1", "https://example/att.jpg"),
                ExpenseSourceContext.Scanner(prefill),
            )
        others.forEach { assertTrue(it.hasSummary(), "expected $it to have a summary") }
    }

    @Test
    fun `every non-gated variant has a distinct title`() {
        val prefill =
            ScannerPrefill(
                merchant = null,
                amountText = null,
                currency = null,
                dateEpochMs = null,
                category = null,
                overallConfidence = 0f,
                duplicateWarning = null,
            )
        val titles =
            listOf(
                ExpenseSourceContext.Edit("exp-1"),
                ExpenseSourceContext.Trip("trip-1"),
                ExpenseSourceContext.TripAdvance("trip-1", "adv-1"),
                ExpenseSourceContext.Event("evt-1"),
                ExpenseSourceContext.Card("card-1", "txn-1"),
                ExpenseSourceContext.Advance("adv-1"),
                ExpenseSourceContext.Message("clar-1", "https://example/att.jpg"),
                ExpenseSourceContext.Scanner(prefill),
            ).map { (it.summaryTitle() as UiText.Res).key }
        assertEqualsSize(titles)
    }

    private fun assertEqualsSize(titles: List<String>) {
        assertNotEquals(0, titles.toSet().size)
        assertTrue(titles.size == titles.toSet().size, "expected every variant to have a distinct title key")
    }
}
