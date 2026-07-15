package com.mileway.core.ui.components.expense

import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.data.model.ScannerPrefill
import com.siddharth.kmp.common.UiText
import kotlin.test.Test
import kotlin.test.assertEquals
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

    // ── P27.E.2: per-variant summary rows ───────────────────────────────────

    @Test
    fun `None and Regular have no summary rows`() {
        assertTrue(ExpenseSourceContext.None.summaryRows().isEmpty())
        assertTrue(ExpenseSourceContext.Regular.summaryRows().isEmpty())
    }

    @Test
    fun `Trip without a label renders only the trip id row`() {
        val rows = ExpenseSourceContext.Trip("trip-1").summaryRows()
        assertEquals(1, rows.size)
        assertEquals("trip-1", rows.single().second)
    }

    @Test
    fun `Trip with a label renders both the label and id rows`() {
        val rows = ExpenseSourceContext.Trip("trip-1", tripLabel = "Mumbai to Pune").summaryRows()
        assertEquals(2, rows.size)
        assertTrue(rows.any { it.second == "Mumbai to Pune" })
        assertTrue(rows.any { it.second == "trip-1" })
    }

    @Test
    fun `Card renders merchant, amount ceiling, card id and transaction id when all known`() {
        val rows =
            ExpenseSourceContext.Card("card-1", "txn-1", merchantName = "Indigo Airlines", transactionAmountRupees = 4500.0)
                .summaryRows()
        assertEquals(4, rows.size)
        assertTrue(rows.any { it.second == "Indigo Airlines" })
        assertTrue(rows.any { it.second == "₹4500.00" })
        assertTrue(rows.any { it.second == "card-1" })
        assertTrue(rows.any { it.second == "txn-1" })
    }

    @Test
    fun `Card without merchant or amount only renders the id rows`() {
        val rows = ExpenseSourceContext.Card("card-1", "txn-1").summaryRows()
        assertEquals(2, rows.size)
    }

    @Test
    fun `Message renders the attachment url`() {
        val rows = ExpenseSourceContext.Message("clar-1", "https://example/att.jpg").summaryRows()
        assertEquals(listOf("https://example/att.jpg"), rows.map { it.second })
    }

    @Test
    fun `Scanner renders confidence even with no other extraction`() {
        val prefill =
            ScannerPrefill(
                merchant = null,
                amountText = null,
                currency = null,
                dateEpochMs = null,
                category = null,
                overallConfidence = 0.5f,
                duplicateWarning = null,
            )
        val rows = ExpenseSourceContext.Scanner(prefill).summaryRows()
        assertEquals(listOf("50%"), rows.map { it.second })
    }
}
