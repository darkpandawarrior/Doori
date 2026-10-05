package com.mileway.feature.approvals.viewmodel

import com.mileway.core.data.claim.ApprovalReview
import com.mileway.core.data.claim.hasHardViolation
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BulkApprovalViewModelTest {
    @BeforeTest
    fun mainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun review(
        id: String,
        flags: List<String> = emptyList(),
    ): ApprovalReview {
        val line = ExpenseLine("$id-line", 100, "INR", policyFlags = flags, merchant = "Cafe", category = "Meals")
        return ApprovalReview(
            Report(id, "employee", listOf(line), ReportLifecycleState.SUBMITTED),
            blockedLineIds = if (line.hasHardViolation()) setOf(line.id) else emptySet(),
        )
    }

    @Test
    fun `bulk selection cannot include hard violation and removes a newly blocked selection`() =
        runTest {
            val queue = MutableStateFlow(listOf(review("hard", listOf("EXPENSE_OVER_MAX")), review("soft", listOf("RECEIPT_RECOMMENDED"))))
            val approved = mutableListOf<String>()
            val vm = BulkApprovalViewModel(queue) { review, _ -> approved.add(review.report.id) }
            advanceUntilIdle()
            vm.select("hard")
            vm.select("soft")
            assertEquals(setOf("soft"), vm.state.value.selectedIds)
            queue.value = listOf(review("soft", listOf("MILEAGE_OVER_POLICY_RATE")))
            advanceUntilIdle()
            assertTrue(
                vm.state.value.selectedIds
                    .isEmpty(),
            )
            vm.comment("Reviewed")
            vm.approveSelected()
            advanceUntilIdle()
            assertTrue(approved.isEmpty())
        }

    @Test
    fun `bulk keeps failed reports selected and never retries completed reports`() =
        runTest {
            val queue = MutableStateFlow(listOf(review("good"), review("stale")))
            val writes = mutableListOf<String>()
            val vm =
                BulkApprovalViewModel(queue) { review, comment ->
                    assertEquals("Checked", comment)
                    if (review.report.id == "stale") error("Report changed; reload before acting")
                    writes.add(review.report.id)
                }
            advanceUntilIdle()
            vm.select("good")
            vm.select("stale")
            vm.approveSelected()
            assertFalse(vm.state.value.busy)
            vm.comment(" Checked ")
            vm.approveSelected()
            vm.approveSelected()
            advanceUntilIdle()
            assertEquals(listOf("good"), writes)
            assertEquals(setOf("stale"), vm.state.value.selectedIds)
            assertEquals(setOf("stale"), vm.state.value.failures.keys)
        }
}
