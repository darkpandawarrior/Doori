package com.mileway

import com.mileway.core.data.claim.ReportPayoutProcessor
import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.domain.claim.ApprovalAction
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
    fun `approve passes actor and comment then pays through existing repository`() = runTest {
        val submitted = Report("report", "employee", state = ReportLifecycleState.SUBMITTED, recordVersion = 4)
        val approved = submitted.copy(state = ReportLifecycleState.APPROVED, recordVersion = 5)
        val reportFlow = MutableStateFlow<Report?>(submitted)
        val reports = mockk<ReportRepository>()
        every { reports.observe("report") } returns reportFlow
        every { reports.observeAll() } returns MutableStateFlow(listOf(submitted))
        coEvery { reports.act("report", 4, "manager", ApprovalAction.APPROVE, "Reviewed", any(), any()) } answers {
            reportFlow.value = approved
            approved
        }
        val payout = mockk<ReportPayoutProcessor>()
        coEvery { payout.pay("report") } returns approved.copy(state = ReportLifecycleState.PAID)
        val payments = PaymentsRepository(reportPayouts = payout)
        val session = object : SessionSource {
            override val sessionState = MutableStateFlow(SessionState(kind = SessionKind.CREDENTIALS, employeeCode = "manager"))
        }
        val viewModel = ReportApprovalViewModel(
            reports, session, FakeClarificationRepository(), ReportPaymentRunner(payments::payReport),
        )
        viewModel.open("report")
        advanceUntilIdle()
        viewModel.comment("Reviewed")
        viewModel.act(ApprovalAction.APPROVE)
        advanceUntilIdle()
        coVerify(exactly = 1) { reports.act("report", 4, "manager", ApprovalAction.APPROVE, "Reviewed", any(), any()) }
        coVerify(exactly = 1) { payout.pay("report") }
        assertNull(viewModel.state.value.error)
        assertEquals("", viewModel.state.value.comment)
    }
}
