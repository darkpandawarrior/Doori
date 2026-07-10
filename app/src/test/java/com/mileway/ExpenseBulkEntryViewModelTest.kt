package com.mileway

import com.mileway.feature.logging.model.DraftStatus
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P27.E.11: a [ExpenseRepository] test double whose `insert` holds a virtual-time [delay] while
 * tracking how many calls are in-flight at once — the smallest way to prove the bulk-submit
 * `Semaphore` genuinely bounds concurrency, rather than merely producing correct end-state.
 */
private class ConcurrencyTrackingExpenseRepository : ExpenseRepository() {
    var maxConcurrent = 0
        private set
    private var inFlight = 0

    override suspend fun insert(record: ExpenseRecord) {
        inFlight++
        maxConcurrent = maxOf(maxConcurrent, inFlight)
        delay(50)
        inFlight--
        super.insert(record)
    }
}

/**
 * H: behavioural coverage for [ExpenseViewModel]'s bulk expense entry grid (P2.1 multi-row grid,
 * P2.2 carry-over defaults/apply-to-all, P2.3 batch submit + retry-failed). Split out of
 * [ExpenseViewModelTest] to keep each test class focused (detekt `LargeClass`).
 */
class ExpenseBulkEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = ExpenseViewModel(ExpenseRepository())

    // ── P2.1: multi-row draft grid for bulk expense entry ──────────────────────

    @Test
    fun `the grid starts with exactly one PENDING row`() {
        val vm = viewModel()
        assertEquals(1, vm.state.value.rows.size)
        assertEquals(DraftStatus.PENDING, vm.state.value.rows.first().status)
    }

    @Test
    fun `AddDraftRow appends a new PENDING row with a distinct id`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.AddDraftRow)
        assertEquals(2, vm.state.value.rows.size)
        val ids = vm.state.value.rows.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(vm.state.value.rows.all { it.status == DraftStatus.PENDING })
    }

    @Test
    fun `DuplicateDraftRow copies field values into a new row next to the source`() {
        val vm = viewModel()
        val sourceId = vm.state.value.rows.first().id
        vm.onAction(ExpenseAction.UpdateDraftRow(sourceId) { it.copy(category = ExpenseCategory.FOOD, merchantName = "Cafe", amountText = "100") })

        vm.onAction(ExpenseAction.DuplicateDraftRow(sourceId))

        val rows = vm.state.value.rows
        assertEquals(2, rows.size)
        val duplicate = rows[1]
        assertEquals(ExpenseCategory.FOOD, duplicate.category)
        assertEquals("Cafe", duplicate.merchantName)
        assertEquals("100", duplicate.amountText)
        assertEquals(DraftStatus.PENDING, duplicate.status)
        assertTrue(duplicate.id != sourceId)
    }

    @Test
    fun `RemoveDraftRow removes a row when more than one remains`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.AddDraftRow)
        val secondId = vm.state.value.rows[1].id

        vm.onAction(ExpenseAction.RemoveDraftRow(secondId))

        assertEquals(1, vm.state.value.rows.size)
        assertTrue(vm.state.value.rows.none { it.id == secondId })
    }

    @Test
    fun `RemoveDraftRow on the last remaining row is a no-op`() {
        val vm = viewModel()
        val onlyId = vm.state.value.rows.first().id

        vm.onAction(ExpenseAction.RemoveDraftRow(onlyId))

        assertEquals(1, vm.state.value.rows.size)
        assertEquals(onlyId, vm.state.value.rows.first().id)
    }

    @Test
    fun `UpdateDraftRow transforms only the targeted row`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.AddDraftRow)
        val firstId = vm.state.value.rows[0].id
        val secondId = vm.state.value.rows[1].id

        vm.onAction(ExpenseAction.UpdateDraftRow(secondId) { it.copy(merchantName = "Uber") })

        assertEquals("", vm.state.value.rows.first { it.id == firstId }.merchantName)
        assertEquals("Uber", vm.state.value.rows.first { it.id == secondId }.merchantName)
    }

    @Test
    fun `AddDraftRow carries over category and merchant from the last row`() {
        val vm = viewModel()
        val firstId = vm.state.value.rows.first().id
        vm.onAction(ExpenseAction.UpdateDraftRow(firstId) { it.copy(category = ExpenseCategory.TRAVEL, merchantName = "Uber") })

        vm.onAction(ExpenseAction.AddDraftRow)

        val secondRow = vm.state.value.rows[1]
        assertEquals(ExpenseCategory.TRAVEL, secondRow.category)
        assertEquals("Uber", secondRow.merchantName)
        assertEquals(DraftStatus.PENDING, secondRow.status)
        assertTrue(secondRow.id != firstId)
    }

    @Test
    fun `ApplyCategoryToAll updates only pending rows, leaving submitted or error rows untouched`() {
        val vm = viewModel()
        val firstId = vm.state.value.rows.first().id
        vm.onAction(ExpenseAction.AddDraftRow)
        val secondId = vm.state.value.rows[1].id
        vm.onAction(ExpenseAction.AddDraftRow)
        val thirdId = vm.state.value.rows[2].id
        vm.onAction(ExpenseAction.UpdateDraftRow(secondId) { it.copy(status = DraftStatus.SUCCESS) })
        vm.onAction(ExpenseAction.UpdateDraftRow(thirdId) { it.copy(status = DraftStatus.ERROR) })

        vm.onAction(ExpenseAction.ApplyCategoryToAll(ExpenseCategory.FOOD))

        val rows = vm.state.value.rows
        assertEquals(ExpenseCategory.FOOD, rows.first { it.id == firstId }.category)
        assertNull(rows.first { it.id == secondId }.category)
        assertNull(rows.first { it.id == thirdId }.category)
    }

    // ── P2.3: local batch submit + per-row outcome + retry-failed ──────────────

    @Test
    fun `SubmitAllDrafts on a fully valid batch marks every row SUCCESS and inserts each record`() = runTest {
        val repository = ExpenseRepository()
        val vm = ExpenseViewModel(repository)
        val before = repository.getAll().size
        val firstId = vm.state.value.rows.first().id
        vm.onAction(ExpenseAction.UpdateDraftRow(firstId) { it.copy(category = ExpenseCategory.FOOD, merchantName = "Cafe A", amountText = "100") })
        vm.onAction(ExpenseAction.AddDraftRow)
        val secondId = vm.state.value.rows[1].id
        vm.onAction(ExpenseAction.UpdateDraftRow(secondId) { it.copy(category = ExpenseCategory.FOOD, merchantName = "Cafe B", amountText = "200") })

        vm.onAction(ExpenseAction.SubmitAllDrafts)
        advanceUntilIdle()

        val rows = vm.state.value.rows
        assertTrue(rows.all { it.status == DraftStatus.SUCCESS })
        assertEquals(before + 2, repository.getAll().size)
        val summary = vm.state.value.submissionSummary
        assertNotNull(summary)
        assertEquals(2, summary.first.size)
        assertTrue(summary.second.isEmpty())
    }

    @Test
    fun `SubmitAllDrafts with one intentionally-invalid row yields two successes and one error`() = runTest {
        val repository = ExpenseRepository()
        val vm = ExpenseViewModel(repository)
        val before = repository.getAll().size
        val firstId = vm.state.value.rows.first().id
        vm.onAction(ExpenseAction.UpdateDraftRow(firstId) { it.copy(category = ExpenseCategory.FOOD, merchantName = "Cafe A", amountText = "100") })
        vm.onAction(ExpenseAction.AddDraftRow)
        val secondId = vm.state.value.rows[1].id
        // Invalid: blank merchant name fails ExpenseFormValidator.
        vm.onAction(ExpenseAction.UpdateDraftRow(secondId) { it.copy(category = ExpenseCategory.FOOD, merchantName = "", amountText = "200") })
        vm.onAction(ExpenseAction.AddDraftRow)
        val thirdId = vm.state.value.rows[2].id
        vm.onAction(ExpenseAction.UpdateDraftRow(thirdId) { it.copy(category = ExpenseCategory.FOOD, merchantName = "Cafe C", amountText = "300") })

        vm.onAction(ExpenseAction.SubmitAllDrafts)
        advanceUntilIdle()

        val rows = vm.state.value.rows
        assertEquals(DraftStatus.SUCCESS, rows.first { it.id == firstId }.status)
        assertEquals(DraftStatus.ERROR, rows.first { it.id == secondId }.status)
        assertEquals(DraftStatus.SUCCESS, rows.first { it.id == thirdId }.status)
        assertEquals(before + 2, repository.getAll().size)

        val summary = vm.state.value.submissionSummary
        assertNotNull(summary)
        assertEquals(2, summary.first.size)
        assertEquals(1, summary.second.size)
        assertEquals(secondId, summary.second.first().id)
    }

    @Test
    fun `SubmitAllDrafts runs rows concurrently but bounded, and preserves every row's own outcome`() = runTest {
        val repository = ConcurrencyTrackingExpenseRepository()
        val vm = ExpenseViewModel(repository)
        val firstId = vm.state.value.rows.first().id
        vm.onAction(ExpenseAction.UpdateDraftRow(firstId) { it.copy(category = ExpenseCategory.FOOD, merchantName = "Row 1", amountText = "10") })
        repeat(6) { i ->
            vm.onAction(ExpenseAction.AddDraftRow)
            val id = vm.state.value.rows.last().id
            // One deliberately-invalid row (blank merchant) among otherwise-valid ones, to prove a
            // single row failing mid-batch doesn't abort or block its concurrently-running siblings.
            val merchant = if (i == 3) "" else "Row ${i + 2}"
            vm.onAction(ExpenseAction.UpdateDraftRow(id) { it.copy(category = ExpenseCategory.FOOD, merchantName = merchant, amountText = "10") })
        }
        assertEquals(7, vm.state.value.rows.size)

        vm.onAction(ExpenseAction.SubmitAllDrafts)
        advanceUntilIdle()

        // Bounded: never more than the Semaphore(4) permit count in flight at once.
        assertTrue(repository.maxConcurrent <= 4, "expected at most 4 concurrent submits, was ${repository.maxConcurrent}")
        // Concurrent: more than one row genuinely overlapped, not a sequential fallback in disguise.
        assertTrue(repository.maxConcurrent > 1, "expected overlapping submits, was ${repository.maxConcurrent}")

        val rows = vm.state.value.rows
        assertEquals(6, rows.count { it.status == DraftStatus.SUCCESS })
        assertEquals(1, rows.count { it.status == DraftStatus.ERROR })
        // submissionSummary keeps its pre-existing (successRows, errorRows) pair shape.
        val summary = vm.state.value.submissionSummary
        assertNotNull(summary)
        assertEquals(6, summary.first.size)
        assertEquals(1, summary.second.size)
    }

    @Test
    fun `RetryFailedDrafts only resubmits rows currently ERROR`() = runTest {
        val repository = ExpenseRepository()
        val vm = ExpenseViewModel(repository)
        val firstId = vm.state.value.rows.first().id
        vm.onAction(ExpenseAction.UpdateDraftRow(firstId) { it.copy(category = ExpenseCategory.FOOD, merchantName = "Cafe A", amountText = "100") })
        vm.onAction(ExpenseAction.AddDraftRow)
        val secondId = vm.state.value.rows[1].id
        vm.onAction(ExpenseAction.UpdateDraftRow(secondId) { it.copy(category = ExpenseCategory.FOOD, merchantName = "", amountText = "200") })

        vm.onAction(ExpenseAction.SubmitAllDrafts)
        advanceUntilIdle()
        assertEquals(DraftStatus.SUCCESS, vm.state.value.rows.first { it.id == firstId }.status)
        assertEquals(DraftStatus.ERROR, vm.state.value.rows.first { it.id == secondId }.status)

        // Fix the error row's merchant name, then retry only the error row.
        vm.onAction(ExpenseAction.UpdateDraftRow(secondId) { it.copy(merchantName = "Cafe B Fixed") })
        val before = repository.getAll().size

        vm.onAction(ExpenseAction.RetryFailedDrafts)
        advanceUntilIdle()

        assertEquals(DraftStatus.SUCCESS, vm.state.value.rows.first { it.id == secondId }.status)
        // First row (already SUCCESS) is untouched — no duplicate insert.
        assertEquals(before + 1, repository.getAll().size)
        val summary = vm.state.value.submissionSummary
        assertNotNull(summary)
        assertEquals(1, summary.first.size)
        assertEquals(secondId, summary.first.first().id)
        assertTrue(summary.second.isEmpty())
    }

    @Test
    fun `RetryFailedDrafts with no ERROR rows publishes an empty summary and touches nothing`() = runTest {
        val repository = ExpenseRepository()
        val vm = ExpenseViewModel(repository)
        val before = repository.getAll().size

        vm.onAction(ExpenseAction.RetryFailedDrafts)
        advanceUntilIdle()

        val summary = vm.state.value.submissionSummary
        assertNotNull(summary)
        assertTrue(summary.first.isEmpty())
        assertTrue(summary.second.isEmpty())
        assertEquals(before, repository.getAll().size)
        assertEquals(DraftStatus.PENDING, vm.state.value.rows.first().status)
    }

    // ── P2.5: per-row receipt attachment via the existing on-device document scanner ─────────

    @Test
    fun `scanning a receipt for one row attaches it to that row only`() {
        val vm = viewModel()
        val firstId = vm.state.value.rows.first().id
        vm.onAction(ExpenseAction.AddDraftRow)
        val secondId = vm.state.value.rows[1].id

        vm.onAction(ExpenseAction.UpdateDraftRow(secondId) { it.copy(receiptImagePath = "content://media/scanned/row2") })

        val rows = vm.state.value.rows
        assertNull(rows.first { it.id == firstId }.receiptImagePath)
        assertEquals("content://media/scanned/row2", rows.first { it.id == secondId }.receiptImagePath)
    }

    @Test
    fun `SubmitAllDrafts carries each row's receiptImagePath through to its resulting record`() = runTest {
        val repository = ExpenseRepository()
        val vm = ExpenseViewModel(repository)
        val firstId = vm.state.value.rows.first().id
        vm.onAction(
            ExpenseAction.UpdateDraftRow(firstId) {
                it.copy(category = ExpenseCategory.FOOD, merchantName = "Cafe A", amountText = "100", receiptImagePath = "content://media/scanned/row1")
            },
        )
        vm.onAction(ExpenseAction.AddDraftRow)
        val secondId = vm.state.value.rows[1].id
        // Second row has no receipt attached — should persist as null, not leak the first row's path.
        vm.onAction(ExpenseAction.UpdateDraftRow(secondId) { it.copy(category = ExpenseCategory.FOOD, merchantName = "Cafe B", amountText = "200") })

        vm.onAction(ExpenseAction.SubmitAllDrafts)
        advanceUntilIdle()

        val insertedForCafeA = repository.getAll().first { it.merchantName == "Cafe A" }
        val insertedForCafeB = repository.getAll().first { it.merchantName == "Cafe B" }
        assertEquals("content://media/scanned/row1", insertedForCafeA.receiptImagePath)
        assertNull(insertedForCafeB.receiptImagePath)
    }

    // ── P2.4: local CSV/TSV bulk-import parser (no backend) ─────────────────────

    @Test
    fun `ImportCsv appends parsed rows to the existing grid without disturbing the starter row`() {
        val vm = viewModel()
        val startId = vm.state.value.rows.first().id
        val csv =
            """
            category,amount,merchant,note
            Food,100,Cafe A,
            Travel,250,Ola Cabs,
            """.trimIndent()

        vm.onAction(ExpenseAction.ImportCsv(csv))

        val rows = vm.state.value.rows
        assertEquals(3, rows.size)
        assertEquals(startId, rows.first().id)
        assertTrue(rows.drop(1).all { it.status == DraftStatus.PENDING })
        assertEquals("Cafe A", rows[1].merchantName)
        assertEquals("Ola Cabs", rows[2].merchantName)
        // Imported rows are re-ided through the ViewModel's own row sequence, never colliding with existing ids.
        assertEquals(rows.size, rows.map { it.id }.toSet().size)
    }

    @Test
    fun `ImportCsv with one malformed row yields one ERROR row among the imported rows`() {
        val vm = viewModel()
        val csv =
            """
            category,amount,merchant,note
            Food,100,Cafe A,
            Travel,not-a-number,Ola Cabs,
            Accommodation,4200,Taj Hotel,
            Office Supplies,899,Staples,
            Other,50,Misc Vendor,
            """.trimIndent()

        vm.onAction(ExpenseAction.ImportCsv(csv))

        val imported = vm.state.value.rows.drop(1)
        assertEquals(5, imported.size)
        assertEquals(1, imported.count { it.status == DraftStatus.ERROR })
        assertEquals(4, imported.count { it.status == DraftStatus.PENDING })
    }

    @Test
    fun `ImportCsv with blank text is a no-op`() {
        val vm = viewModel()
        val before = vm.state.value.rows

        vm.onAction(ExpenseAction.ImportCsv(""))

        assertEquals(before, vm.state.value.rows)
    }
}
