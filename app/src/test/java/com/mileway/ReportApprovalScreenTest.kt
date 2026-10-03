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
import com.mileway.core.data.claim.ReportPayoutProcessor
import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.domain.claim.ApprovalAction
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
    fun `comment and approve button drive the existing payment path to paid`() {
        val submitted =
            Report(
                "report",
                "employee",
                listOf(ExpenseLine("line", 5000, "INR", merchant = "Cafe", category = "Meals")),
                state = ReportLifecycleState.SUBMITTED,
                recordVersion = 4,
            )
        val reportFlow = MutableStateFlow<Report?>(submitted)
        val reports = mockk<ReportRepository>()
        every { reports.observe("report") } returns reportFlow
        every { reports.observeAll() } returns MutableStateFlow(listOf(submitted))
        coEvery { reports.act("report", 4, "manager", ApprovalAction.APPROVE, "Reviewed", any(), any()) } answers {
            submitted.copy(state = ReportLifecycleState.APPROVED).also { reportFlow.value = it }
        }
        val payouts = mockk<ReportPayoutProcessor>()
        coEvery { payouts.pay("report") } answers {
            submitted.copy(state = ReportLifecycleState.PAID).also { reportFlow.value = it }
        }
        val payments = PaymentsRepository(reportPayouts = payouts)
        val session =
            object : SessionSource {
                override val sessionState = MutableStateFlow(SessionState(kind = SessionKind.CREDENTIALS, employeeCode = "manager"))
            }
        val viewModel = ReportApprovalViewModel(reports, session, FakeClarificationRepository(), ReportPaymentRunner(payments::payReport))
        composeRule.setContent { MaterialTheme { ReportApprovalScreen("report", onBack = {}, viewModel = viewModel) } }
        composeRule.onNodeWithText("Approval comment (required)").performScrollTo().performTextInput("Reviewed")
        composeRule.onNodeWithText("Approve and simulate payout").performScrollTo().performClick()
        composeRule.onNodeWithText("Status: PAID").assertIsDisplayed()
        coVerify(exactly = 1) { payouts.pay("report") }
    }
}
