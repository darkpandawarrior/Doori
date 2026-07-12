package com.mileway

import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.model.ApprovalType
import com.mileway.feature.approvals.repository.ApprovalsRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApprovalsTest {

    @Test
    fun `all has 12 deterministic entries`() {
        assertEquals(12, ApprovalsRepository.all.size)
    }

    @Test
    fun `ids are unique`() {
        val ids = ApprovalsRepository.all.map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `getById returns correct item`() {
        val item = ApprovalsRepository.getById("A001")
        assertNotNull(item)
        assertEquals("A001", item.id)
        assertEquals(ApprovalType.MILEAGE, item.type)
        assertEquals(ApprovalStatus.PENDING, item.status)
    }

    @Test
    fun `getById returns null for unknown id`() {
        assertNull(ApprovalsRepository.getById("UNKNOWN"))
    }

    @Test
    fun `pending count is correct`() {
        val pending = ApprovalsRepository.all.count { it.status == ApprovalStatus.PENDING }
        assertEquals(4, pending)
    }

    @Test
    fun `approved count is correct`() {
        val approved = ApprovalsRepository.all.count { it.status == ApprovalStatus.APPROVED }
        assertEquals(6, approved)
    }

    @Test
    fun `rejected count is correct`() {
        val rejected = ApprovalsRepository.all.count { it.status == ApprovalStatus.REJECTED }
        assertEquals(2, rejected)
    }

    @Test
    fun `all approval types are represented`() {
        val types = ApprovalsRepository.all.map { it.type }.toSet()
        assertTrue(ApprovalType.MILEAGE in types)
        assertTrue(ApprovalType.EXPENSE in types)
        assertTrue(ApprovalType.TRAVEL in types)
        assertTrue(ApprovalType.ADVANCE in types)
    }

    @Test
    fun `approve mutates only the targeted item`() {
        val updated = ApprovalsRepository.approve("A001")
        assertEquals(ApprovalStatus.APPROVED, updated.first { it.id == "A001" }.status)
        // All others unchanged from original
        ApprovalsRepository.all.filter { it.id != "A001" }.forEach { original ->
            val inUpdated = updated.first { it.id == original.id }
            assertEquals(original.status, inUpdated.status)
        }
    }

    @Test
    fun `reject mutates only the targeted item`() {
        val updated = ApprovalsRepository.reject("A004")
        assertEquals(ApprovalStatus.REJECTED, updated.first { it.id == "A004" }.status)
        ApprovalsRepository.all.filter { it.id != "A004" }.forEach { original ->
            val inUpdated = updated.first { it.id == original.id }
            assertEquals(original.status, inUpdated.status)
        }
    }

    @Test
    fun `approve and reject return lists of same size as all`() {
        assertEquals(ApprovalsRepository.all.size, ApprovalsRepository.approve("A001").size)
        assertEquals(ApprovalsRepository.all.size, ApprovalsRepository.reject("A001").size)
    }

    @Test
    fun `all items have positive amounts`() {
        ApprovalsRepository.all.forEach { item ->
            assertTrue(item.amountRupees > 0.0, "Item ${item.id} has non-positive amount")
        }
    }

    @Test
    fun `items are deterministic across accesses`() {
        val first = ApprovalsRepository.all.map { it.id }
        val second = ApprovalsRepository.all.map { it.id }
        assertEquals(first, second)
    }
}
