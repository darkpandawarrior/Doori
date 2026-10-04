package com.mileway

import com.mileway.core.data.claim.ReportPayoutProcessor
import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.claim.FINANCE_ROLE
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ApprovalChain
import com.mileway.core.data.domain.claim.ApprovalStep
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.payout.ReportPaymentRunner
import com.mileway.core.data.session.SessionKind
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.session.SessionState
import com.mileway.feature.approvals.repository.FakeClarificationRepository
import com.mileway.feature.approvals.viewmodel.ReportApprovalViewModel
import com.mileway.feature.payments.repository.PaymentsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ReportApprovalFlowTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `manager approval waits for finance before paying through existing repository`() =
        runTest {
            val submitted = Report("report", "employee", state = ReportLifecycleState.SUBMITTED, recordVersion = 4)
            val manager = submitted.copy(recordVersion = 5, approvalChain = ApprovalChain(listOf(
                ApprovalStep(0, actedBy = "manager", action = ApprovalAction.APPROVE, actedAtMillis = 1),
            )))
            val approved = manager.copy(state = ReportLifecycleState.APPROVED, recordVersion = 6)
            val reportFlow = MutableStateFlow<Report?>(submitted)
            val reports = mockk<ReportRepository>()
            every { reports.observe("report") } returns reportFlow
            every { reports.observeAll() } returns MutableStateFlow(listOf(submitted))
            coEvery { reports.review("report") } answers { reportFlow.value?.let(::approvalReviewOf) }
            coEvery { reports.delegates("report", any()) } returns emptyList()
            coEvery { reports.act("report", 4, "manager", ApprovalAction.APPROVE, "Reviewed", "manager", null) } answers {
                reportFlow.value = manager
                manager
            }
            coEvery { reports.act("report", 5, "finance", ApprovalAction.APPROVE, "Finance checked", FINANCE_ROLE, null) } answers {
                reportFlow.value = approved
                approved
            }
            val payout = mockk<ReportPayoutProcessor>()
            coEvery { payout.pay("report") } returns approved.copy(state = ReportLifecycleState.PAID)
            val payments = PaymentsRepository(reportPayouts = payout)
            val sessionFlow = MutableStateFlow(SessionState(kind = SessionKind.CREDENTIALS, employeeCode = "manager"))
            val session =
                object : SessionSource {
                    override val sessionState = sessionFlow
                }
            val viewModel =
                ReportApprovalViewModel(
                    reports,
                    session,
                    FakeClarificationRepository(),
                    ReportPaymentRunner(payments::payReport),
                )
            viewModel.open("report")
            advanceUntilIdle()
            viewModel.comment("Reviewed")
            viewModel.act(ApprovalAction.APPROVE)
            advanceUntilIdle()
            coVerify(exactly = 0) { payout.pay("report") }
            assertEquals(ReportLifecycleState.SUBMITTED, viewModel.state.value.report?.state)
            sessionFlow.value = sessionFlow.value.copy(employeeCode = "finance")
            viewModel.comment("Finance checked")
            viewModel.act(ApprovalAction.APPROVE)
            advanceUntilIdle()
            coVerify(exactly = 1) { reports.act("report", 4, "manager", ApprovalAction.APPROVE, "Reviewed", any(), any()) }
            coVerify(exactly = 1) { reports.act("report", 5, "finance", ApprovalAction.APPROVE, "Finance checked", FINANCE_ROLE, null) }
            coVerify(exactly = 1) { payout.pay("report") }
            assertNull(viewModel.state.value.error)
            assertEquals("", viewModel.state.value.comment)
        }
}
