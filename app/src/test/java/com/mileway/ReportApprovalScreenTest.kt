package com.mileway

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.data.claim.FINANCE_ROLE
import com.mileway.core.data.claim.ReportPayoutProcessor
import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ApprovalChain
import com.mileway.core.data.domain.claim.ApprovalStep
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.payout.ReportPaymentRunner
import com.mileway.core.data.session.SessionKind
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.session.SessionState
import com.mileway.feature.approvals.repository.FakeClarificationRepository
import com.mileway.feature.approvals.ui.screens.ReportApprovalScreen
import com.mileway.feature.approvals.viewmodel.ReportApprovalViewModel
import com.mileway.feature.payments.repository.PaymentsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class)
class ReportApprovalScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalResourceApi::class)
    @Before
    fun resources() {
        ComposeResourcesTestFixture.install()
        setResourceReaderAndroidContext(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `manager then finance buttons drive the existing payment path to paid`() {
        val submitted =
            Report(
                "report",
                "employee",
                listOf(ExpenseLine("line", 5000, "INR", merchant = "Cafe", category = "Meals")),
                state = ReportLifecycleState.SUBMITTED,
                recordVersion = 4,
            )
        val manager =
            submitted.copy(
                recordVersion = 5,
                approvalChain =
                    ApprovalChain(
                        listOf(
                            ApprovalStep(0, actedBy = "manager", action = ApprovalAction.APPROVE, actedAtMillis = 1),
                        ),
                    ),
            )
        val reportFlow = MutableStateFlow<Report?>(submitted)
        val reports = mockk<ReportRepository>()
        every { reports.observe("report") } returns reportFlow
        every { reports.observeAll() } returns MutableStateFlow(listOf(submitted))
        coEvery { reports.review("report") } answers { reportFlow.value?.let(::approvalReviewOf) }
        coEvery { reports.delegates("report", any()) } returns emptyList()
        coEvery { reports.act("report", 4, "manager", ApprovalAction.APPROVE, "Reviewed", "manager", null) } answers {
            manager.also { reportFlow.value = it }
        }
        coEvery { reports.act("report", 5, "finance", ApprovalAction.APPROVE, "Finance checked", FINANCE_ROLE, null) } answers {
            manager.copy(state = ReportLifecycleState.APPROVED, recordVersion = 6).also { reportFlow.value = it }
        }
        val payouts = mockk<ReportPayoutProcessor>()
        coEvery { payouts.pay("report") } answers {
            submitted.copy(state = ReportLifecycleState.PAID).also { reportFlow.value = it }
        }
        val payments = PaymentsRepository(reportPayouts = payouts)
        val sessionFlow = MutableStateFlow(SessionState(kind = SessionKind.CREDENTIALS, employeeCode = "manager"))
        val session =
            object : SessionSource {
                override val sessionState = sessionFlow
            }
        val viewModel = ReportApprovalViewModel(reports, session, FakeClarificationRepository(), ReportPaymentRunner(payments::payReport))
        composeRule.setContent { MaterialTheme { ReportApprovalScreen("report", onBack = {}, viewModel = viewModel) } }
        composeRule.onNodeWithText("Approval comment (required)").performScrollTo().performTextInput("Reviewed")
        composeRule.onNodeWithText("Approve for finance review").performScrollTo().performClick()
        composeRule.onNodeWithText("Next review: FINANCE").performScrollTo().assertIsDisplayed()
        coVerify(exactly = 0) { payouts.pay("report") }
        sessionFlow.value = sessionFlow.value.copy(employeeCode = "finance")
        composeRule.onNodeWithText("Approval comment (required)").performScrollTo().performTextInput("Finance checked")
        composeRule.onNodeWithText("Finance approve and simulate payout").performScrollTo().performClick()
        composeRule.onNodeWithText("Status: PAID").assertIsDisplayed()
        coVerify(exactly = 1) { payouts.pay("report") }
    }
}
