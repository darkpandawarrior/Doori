package com.mileway.feature.cards

import com.mileway.core.data.domain.claim.CostSplit
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.FxRate
import com.mileway.core.data.domain.claim.FxRateSource
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.SplitTarget
import com.mileway.core.data.domain.claim.amountInCurrencyMinor
import com.mileway.core.data.model.db.StatementImportEntity
import com.mileway.core.network.fx.FxRatePinner
import com.mileway.feature.cards.import.StatementImporter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StatementImporterTest {
    private val domestic = ExpenseLine("line", 12000, "INR", incurredOn = "2026-09-25", merchant = "Cafe Orchard", category = "FOOD")

    private class Store(
        var reports: List<Report>,
    ) {
        val batches = mutableMapOf<String, StatementImportEntity>()
        val saved = mutableMapOf<String, ExpenseLine>()
        val importer =
            StatementImporter(
                loadReports = { reports },
                batchExists = { it in batches },
                saveMatches = { batch, snapshots, matches ->
                    batches[batch.id] = batch
                    saved.putAll(matches)
                    reports = snapshots.map { report -> report.copy(lines = report.lines.map { matches[it.id] ?: it }) }
                    true
                },
                pinner = FxRatePinner(reference = { _, _, _ -> error("No network/reference rate on import") }, nowMillis = { 42 }),
            )
    }

    @Test
    fun matchingLocksOriginalAmountAndReimportDoesNotWriteAgain() =
        runTest {
            val store = Store(listOf(Report("r", "employee", listOf(domestic.copy(splits = listOf(CostSplit(SplitTarget.PROJECT, "p", 10000, 12000)))))))
            val csv = "id,date,merchant,amount,currency\ntx1,2026-09-25,Cafe Orchard,120.50,INR"
            val result = store.importer.import("employee", "card.csv", csv)
            val matched = store.saved.getValue("line")
            assertEquals(1, result.matched)
            assertEquals(12000, matched.amountMinor)
            assertEquals(matched.amountMinor, matched.splits.sumOf { it.amountMinor })
            assertNotNull(matched.cardMatchId)
            assertTrue(store.importer.import("employee", "renamed.csv", csv).alreadyImported)
            assertEquals(1, store.batches.size)
            val overlap = store.importer.import("employee", "next.csv", csv + "\ntx2,2026-09-25,Cafe Orchard,120.00,INR")
            assertEquals(0, overlap.matched)
            assertEquals("Transaction already matched", overlap.rows.first().reason)
        }

    @Test
    fun foreignOriginalAmountSetsCardMatchFxAndKeepsSplitAnchor() =
        runTest {
            val foreign = domestic.copy(amountMinor = 1000, currency = "USD", splits = listOf(CostSplit(SplitTarget.PROJECT, "p", 10000, 1000)))
            val store = Store(listOf(Report("r", "employee", listOf(foreign))))
            val result =
                store.importer.import(
                    "employee",
                    "foreign.csv",
                    "id,date,merchant,amount,currency,foreign_amount,foreign_currency\ntx,2026-09-26,Cafe Orchard,850.00,INR,10.00,USD",
                )
            val matched = store.saved.getValue("line")
            assertEquals(1, result.matched)
            assertNotNull(matched.cardMatchId)
            assertEquals(FxRateSource.CARD_MATCHED, matched.fxRate?.source)
            assertEquals("2026-09-26", matched.fxRate?.sourceDate)
            assertEquals(85.0, matched.fxRate?.rate)
            assertEquals(42, matched.fxRatePinnedAt)
            assertEquals(1000, matched.amountMinor)
            assertEquals(1000, matched.splits.sumOf { it.amountMinor })
            assertEquals(85000, matched.amountInCurrencyMinor("INR"))
        }

    @Test
    fun billedInrCanMatchThroughAnExistingPinButNotAnUnpinnedForeignLine() =
        runTest {
            val foreign =
                domestic.copy(
                    amountMinor = 1000,
                    currency = "USD",
                    fxRate = FxRate(85.0, "USD", sourceDate = "2026-09-25"),
                    fxRatePinnedAt = 1,
                )
            val store = Store(listOf(Report("r", "employee", listOf(foreign))))
            assertEquals(1, store.importer.import("employee", "pinned.csv", "id,date,merchant,amount,currency\ntx,2026-09-25,Cafe Orchard,850.00,INR").matched)
            assertEquals(
                FxRateSource.CARD_MATCHED,
                store.saved
                    .getValue("line")
                    .fxRate
                    ?.source,
            )
            val noPin = Store(listOf(Report("r", "employee", listOf(foreign.copy(fxRatePinnedAt = null)))))
            assertEquals(
                0,
                noPin.importer.import("employee", "unresolved.csv", "id,date,merchant,amount,currency\ntx,2026-09-25,Cafe Orchard,10.00,INR").matched,
            )
            assertTrue(noPin.saved.isEmpty())
        }

    @Test
    fun twoRowsCannotConsumeTheSameClaim() =
        runTest {
            val store = Store(listOf(Report("r", "employee", listOf(domestic))))
            val result =
                store.importer.import(
                    "employee",
                    "two.csv",
                    "id,date,merchant,amount,currency\na,2026-09-25,Cafe Orchard,120.00,INR\nb,2026-09-25,Cafe Orchard,120.00,INR",
                )
            assertEquals(1, result.matched)
            assertEquals(1, store.saved.size)
        }
}
