package com.mileway

import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.repository.ExpenseRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExpenseTest {

    private val repo = ExpenseRepository()

    @Test
    fun `getAll returns 8 deterministic records`() {
        assertEquals(8, repo.getAll().size)
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
}
