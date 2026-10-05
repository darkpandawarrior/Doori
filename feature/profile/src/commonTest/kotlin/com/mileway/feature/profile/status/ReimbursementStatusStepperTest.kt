package com.mileway.feature.profile.status

import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.session.SessionKind
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.session.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReimbursementStatusStepperTest {
    @Test
    fun `every persisted state has an honest milestone`() {
        val expected =
            mapOf(
                ReportLifecycleState.DRAFT to 1,
                ReportLifecycleState.SUBMITTED to 2,
                ReportLifecycleState.APPROVED to 3,
                ReportLifecycleState.APPROVED_FOR_PAYMENT to 4,
                ReportLifecycleState.PAID to 5,
                ReportLifecycleState.SENT_BACK to 1,
                ReportLifecycleState.RECALLED to 1,
                ReportLifecycleState.REJECTED to 2,
            )
        assertEquals(ReportLifecycleState.entries.toSet(), expected.keys)
        expected.forEach { (state, milestone) -> assertEquals(milestone, reimbursementProgress(state).milestone) }
        assertTrue(reimbursementProgress(ReportLifecycleState.REJECTED).message.contains("Rejected"))
        assertTrue(reimbursementProgress(ReportLifecycleState.APPROVED_FOR_PAYMENT).message.contains("simulated"))
    }

    @Test
    fun `live lifecycle changes and account switches replace employee rows`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val identity = MutableStateFlow(SessionState(kind = SessionKind.CREDENTIALS, employeeCode = "employee-a"))
                val a = MutableStateFlow(listOf(Report("a", "employee-a")))
                val b = MutableStateFlow(listOf(Report("b", "employee-b")))
                val session =
                    object : SessionSource {
                        override val sessionState = identity
                    }
                val vm = ReimbursementStatusViewModel({ if (it == "employee-a") a else b }, session)
                advanceUntilIdle()
                assertEquals(
                    "a",
                    vm.state.value.reports
                        .single()
                        .id,
                )
                a.value = listOf(a.value.single().copy(state = ReportLifecycleState.PAID))
                advanceUntilIdle()
                assertEquals(
                    5,
                    reimbursementProgress(
                        vm.state.value.reports
                            .single()
                            .state,
                    ).milestone,
                )
                identity.value = identity.value.copy(employeeCode = "employee-b")
                advanceUntilIdle()
                assertEquals(
                    "b",
                    vm.state.value.reports
                        .single()
                        .id,
                )
                a.value = emptyList()
                advanceUntilIdle()
                assertEquals(
                    "b",
                    vm.state.value.reports
                        .single()
                        .id,
                )
                identity.value = SessionState()
                advanceUntilIdle()
                assertTrue(
                    vm.state.value.reports
                        .isEmpty(),
                )
                assertEquals("Sign in to view reimbursements", vm.state.value.error)
            } finally {
                Dispatchers.resetMain()
            }
        }
}
