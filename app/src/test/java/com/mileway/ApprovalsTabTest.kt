package com.mileway

import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.repository.ApprovalsRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApprovalsTabTest {

    @Test
    fun `teamItems has exactly 3 entries`() {
        assertEquals(3, ApprovalsRepository.teamItems.size)
    }

    @Test
    fun `all teamItems are PENDING`() {
        ApprovalsRepository.teamItems.forEach { item ->
            assertEquals(
                ApprovalStatus.PENDING,
                item.status,
                "Team item ${item.id} expected PENDING but was ${item.status}"
            )
        }
    }

    @Test
    fun `teamItem ids start with T prefix`() {
        ApprovalsRepository.teamItems.forEach { item ->
            assertTrue(item.id.startsWith("T"), "Team item ${item.id} should start with T")
        }
    }

    @Test
    fun `myRequests has exactly 4 entries`() {
        assertEquals(4, ApprovalsRepository.myRequests.size)
    }

    @Test
    fun `myRequests has exactly 1 REJECTED entry`() {
        val rejectedCount = ApprovalsRepository.myRequests.count { it.status == ApprovalStatus.REJECTED }
        assertEquals(1, rejectedCount)
    }

    @Test
    fun `myRequests has exactly 2 APPROVED entries`() {
        val approvedCount = ApprovalsRepository.myRequests.count { it.status == ApprovalStatus.APPROVED }
        assertEquals(2, approvedCount)
    }

    @Test
    fun `myRequests has exactly 1 PENDING entry`() {
        val pendingCount = ApprovalsRepository.myRequests.count { it.status == ApprovalStatus.PENDING }
        assertEquals(1, pendingCount)
    }

    @Test
    fun `myRequest ids start with R prefix`() {
        ApprovalsRepository.myRequests.forEach { item ->
            assertTrue(item.id.startsWith("R"), "My-request item ${item.id} should start with R")
        }
    }

    @Test
    fun `all myRequests have non-blank summaries`() {
        ApprovalsRepository.myRequests.forEach { item ->
            assertTrue(item.summary.isNotBlank(), "My-request ${item.id} has blank summary")
        }
    }

    @Test
    fun `myRequests and teamItems ids do not overlap`() {
        val teamIds = ApprovalsRepository.teamItems.map { it.id }.toSet()
        val myIds = ApprovalsRepository.myRequests.map { it.id }.toSet()
        assertTrue(teamIds.intersect(myIds).isEmpty(), "Team and my-request ids must not overlap")
    }

    @Test
    fun `myRequests and teamItems ids do not overlap with all`() {
        val allIds = ApprovalsRepository.all.map { it.id }.toSet()
        val tabIds = (ApprovalsRepository.teamItems + ApprovalsRepository.myRequests).map { it.id }.toSet()
        assertTrue(allIds.intersect(tabIds).isEmpty(), "Tab-specific ids must be distinct from the main approval list")
    }

    @Test
    fun `all tab items have positive amounts`() {
        (ApprovalsRepository.teamItems + ApprovalsRepository.myRequests).forEach { item ->
            assertTrue(item.amountRupees > 0.0, "Item ${item.id} has non-positive amount")
        }
    }

    @Test
    fun `data is deterministic across accesses`() {
        val teamFirst = ApprovalsRepository.teamItems.map { it.id }
        val teamSecond = ApprovalsRepository.teamItems.map { it.id }
        assertEquals(teamFirst, teamSecond)

        val myFirst = ApprovalsRepository.myRequests.map { it.id }
        val mySecond = ApprovalsRepository.myRequests.map { it.id }
        assertEquals(myFirst, mySecond)
    }
}
