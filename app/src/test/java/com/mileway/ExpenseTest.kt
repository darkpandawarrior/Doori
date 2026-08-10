package com.mileway

import com.mileway.core.data.model.db.DraftExpenseEntity
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.logging.repository.PERSISTED_DRAFT_RECORD_ID
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExpenseTest {

    private val repo = ExpenseRepository()

    @Test
    fun `getAll returns 9 deterministic records`() {
        // Was 8. A ninth fixture was added deliberately this session to give the expense list a
        // realistic filled state — the screens looked sparse in screenshots because the fixtures
        // were thin, not because the screens were bad. The count is pinned on purpose: fixtures
        // feed screenshot baselines, so a silent change to them silently changes what the gallery
        // claims the app looks like.
        assertEquals(9, repo.getAll().size)
    }

    @Test
    fun `ids are unique`() {
        val ids = repo.getAll().map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `getById returns correct record`() {
        val expense = repo.getById("EXP-001")
        assertNotNull(expense)
        assertEquals("EXP-001", expense.id)
        assertEquals(ExpenseStatus.APPROVED, expense.status)
    }

    @Test
    fun `getById returns null for unknown id`() {
        assertNull(repo.getById("EXP-999"))
    }

    @Test
    fun `filterByStatus null returns all records`() {
        assertEquals(repo.getAll().size, repo.filterByStatus(null).size)
    }

    @Test
    fun `filterByStatus APPROVED returns only approved records`() {
        val approved = repo.filterByStatus(ExpenseStatus.APPROVED)
        assertTrue(approved.isNotEmpty())
        assertTrue(approved.all { it.status == ExpenseStatus.APPROVED })
    }

    @Test
    fun `filterByStatus DRAFT returns only draft records`() {
        val drafts = repo.filterByStatus(ExpenseStatus.DRAFT)
        assertTrue(drafts.isNotEmpty())
        assertTrue(drafts.all { it.status == ExpenseStatus.DRAFT })
    }

    @Test
    fun `all records have positive amounts`() {
        repo.getAll().forEach { expense ->
            assertTrue(expense.amountRupees > 0.0, "${expense.id} has non-positive amount")
        }
    }

    @Test
    fun `requiresApproval is true for amounts over 5000`() {
        val expensive = repo.getAll().filter { it.amountRupees > 5000.0 }
        assertTrue(expensive.isNotEmpty(), "Expected at least one record > ₹5,000")
        expensive.forEach { assertTrue(it.requiresApproval, "${it.id} should require approval") }
    }

    @Test
    fun `requiresApproval is false for amounts at or below 5000`() {
        val cheap = repo.getAll().filter { it.amountRupees <= 5000.0 }
        assertTrue(cheap.isNotEmpty(), "Expected at least one record <= ₹5,000")
        cheap.forEach { assertTrue(!it.requiresApproval, "${it.id} should not require approval") }
    }

    @Test
    fun `getAll is deterministic across two instances`() {
        val repo2 = ExpenseRepository()
        assertEquals(repo.getAll().map { it.id }, repo2.getAll().map { it.id })
    }

    @Test
    fun `all records have non-blank merchant names`() {
        repo.getAll().forEach { expense ->
            assertTrue(expense.merchantName.isNotBlank(), "${expense.id} has blank merchant name")
        }
    }

    @Test
    fun `insert appends a new record and getAll reflects it`() =
        runTest {
            val before = repo.getAll().size
            val new =
                ExpenseRecord(
                    id = "EXP-NEW-1",
                    category = ExpenseCategory.FOOD,
                    merchantName = "Test Cafe",
                    amountRupees = 199.0,
                    status = ExpenseStatus.PENDING,
                    dateMs = 0L,
                )
            repo.insert(new)
            assertEquals(before + 1, repo.getAll().size)
            assertEquals(new, repo.getById("EXP-NEW-1"))
        }

    @Test
    fun `insert with an existing id replaces that record instead of duplicating it`() =
        runTest {
            val before = repo.getAll().size
            val replacement = repo.getById("EXP-001")!!.copy(merchantName = "Updated Merchant")
            repo.insert(replacement)
            assertEquals(before, repo.getAll().size)
            assertEquals("Updated Merchant", repo.getById("EXP-001")?.merchantName)
        }

    @Test
    fun `update replaces an existing record's fields`() =
        runTest {
            val updated = repo.getById("EXP-002")!!.copy(status = ExpenseStatus.APPROVED, amountRupees = 7000.0)
            repo.update(updated)
            val result = repo.getById("EXP-002")
            assertNotNull(result)
            assertEquals(ExpenseStatus.APPROVED, result.status)
            assertEquals(7000.0, result.amountRupees)
        }

    @Test
    fun `update for an unknown id is a no-op`() =
        runTest {
            val before = repo.getAll()
            val ghost =
                ExpenseRecord(
                    id = "EXP-DOES-NOT-EXIST",
                    category = ExpenseCategory.OTHER,
                    merchantName = "Ghost",
                    amountRupees = 1.0,
                    status = ExpenseStatus.DRAFT,
                    dateMs = 0L,
                )
            repo.update(ghost)
            assertEquals(before, repo.getAll())
            assertNull(repo.getById("EXP-DOES-NOT-EXIST"))
        }

    @Test
    fun `receiptImagePath defaults to null when not supplied`() {
        val record =
            ExpenseRecord(
                id = "EXP-NEW-2",
                category = ExpenseCategory.FOOD,
                merchantName = "Test Cafe",
                amountRupees = 100.0,
                status = ExpenseStatus.PENDING,
                dateMs = 0L,
            )
        assertNull(record.receiptImagePath)
    }

    @Test
    fun `insert persists a record with an attached receipt image path`() =
        runTest {
            val withReceipt =
                ExpenseRecord(
                    id = "EXP-NEW-3",
                    category = ExpenseCategory.TRAVEL,
                    merchantName = "Ola Cabs",
                    amountRupees = 500.0,
                    status = ExpenseStatus.PENDING,
                    dateMs = 0L,
                    receiptImagePath = "content://media/picked/1",
                )
            repo.insert(withReceipt)
            assertEquals("content://media/picked/1", repo.getById("EXP-NEW-3")?.receiptImagePath)
        }

    @Test
    fun `saveDraft persists to the dao and unions it into getAll`() =
        runTest {
            val dao = FakeDraftExpenseDao()
            val repository = ExpenseRepository(dao)
            val before = repository.getAll().size
            val draft =
                DraftExpenseEntity(
                    categoryName = ExpenseCategory.TRAVEL.name,
                    amountText = "450.0",
                    merchantName = "Uber: Airport",
                    note = "",
                    receiptImagePath = null,
                    updatedAt = 123L,
                )
            repository.saveDraft(draft)
            assertEquals(before + 1, repository.getAll().size)
            val unioned = repository.getById(PERSISTED_DRAFT_RECORD_ID)
            assertNotNull(unioned)
            assertEquals(ExpenseStatus.DRAFT, unioned.status)
            assertEquals("Uber: Airport", unioned.merchantName)
            assertEquals(450.0, unioned.amountRupees)
            assertEquals(draft, dao.getDraft())
        }

    @Test
    fun `saveDraft twice replaces the unioned record instead of duplicating it`() =
        runTest {
            val dao = FakeDraftExpenseDao()
            val repository = ExpenseRepository(dao)
            val before = repository.getAll().size
            repository.saveDraft(
                DraftExpenseEntity(categoryName = null, amountText = "10", merchantName = "A", note = "", receiptImagePath = null, updatedAt = 1L),
            )
            repository.saveDraft(
                DraftExpenseEntity(categoryName = null, amountText = "20", merchantName = "B", note = "", receiptImagePath = null, updatedAt = 2L),
            )
            assertEquals(before + 1, repository.getAll().size)
            assertEquals("B", repository.getById(PERSISTED_DRAFT_RECORD_ID)?.merchantName)
        }

    @Test
    fun `loadDraft returns what saveDraft persisted, surviving a fresh repository instance (kill+relaunch)`() =
        runTest {
            val dao = FakeDraftExpenseDao()
            val repository = ExpenseRepository(dao)
            val draft =
                DraftExpenseEntity(
                    categoryName = ExpenseCategory.FOOD.name,
                    amountText = "199.0",
                    merchantName = "Cafe Coffee Day",
                    note = "team snacks",
                    receiptImagePath = null,
                    updatedAt = 42L,
                )
            repository.saveDraft(draft)

            // Simulate app relaunch: a brand-new repository instance backed by the same dao.
            val relaunched = ExpenseRepository(dao)
            assertEquals(draft, relaunched.loadDraft())
        }

    @Test
    fun `clearDraft removes the persisted draft and the unioned record`() =
        runTest {
            val dao = FakeDraftExpenseDao()
            val repository = ExpenseRepository(dao)
            val before = repository.getAll().size
            repository.saveDraft(
                DraftExpenseEntity(categoryName = null, amountText = "10", merchantName = "A", note = "", receiptImagePath = null, updatedAt = 1L),
            )
            repository.clearDraft()
            assertEquals(before, repository.getAll().size)
            assertNull(repository.getById(PERSISTED_DRAFT_RECORD_ID))
            assertNull(repository.loadDraft())
        }

    @Test
    fun `loadDraft returns null when no draft was ever saved`() =
        runTest {
            val repository = ExpenseRepository(FakeDraftExpenseDao())
            assertNull(repository.loadDraft())
        }

    @Test
    fun `saveDraft and clearDraft are no-ops on a repository with no dao`() =
        runTest {
            // Default constructor (no draftDao) — matches every other ExpenseRepositoryTest in
            // this file; must not throw even though there's nowhere to persist to.
            val before = repo.getAll().size
            repo.saveDraft(
                DraftExpenseEntity(categoryName = null, amountText = "1", merchantName = "X", note = "", receiptImagePath = null, updatedAt = 0L),
            )
            assertEquals(before + 1, repo.getAll().size)
            repo.clearDraft()
            assertEquals(before, repo.getAll().size)
        }
}
