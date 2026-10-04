package com.mileway

import com.mileway.core.data.claim.ApprovalReview
import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.ApprovalStepEntity
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.session.SessionKind
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.session.SessionState
import com.mileway.feature.approvals.viewmodel.PerLineReviewViewModel
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
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class PerLineReviewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `mileage review loads source route and rejects only selected line with dual attribution`() = runTest {
        val mileage = MileageLine("mileage", 1000, "INR", sourceTripId = "trip", distanceKm = 10.0, vehicleKey = "car")
        val report = Report("report", "employee", listOf(mileage,
            ExpenseLine("expense", 500, "INR", merchant = "Cafe", category = "Meals")), ReportLifecycleState.SUBMITTED, recordVersion = 3)
        val source = MutableStateFlow<Report?>(report)
        var review = ApprovalReview(report)
        val route = listOf(LocationData(activity = "DRIVING", speed = 0f, lat = 18.5, lng = 73.8, token = "trip", batteryPercentage = 50.0))
        val reports = mockk<ReportRepository>()
        every { reports.observe("report") } returns source
        coEvery { reports.review("report") } answers { review }
        coEvery { reports.mileageRoute(mileage) } returns route
        coEvery { reports.reviewLine("report", 3, "proxy", ApprovalAction.REJECT, "Duplicate", "mileage", "manager") } answers {
            val updated = report.copy(recordVersion = 4)
            review = ApprovalReview(updated, listOf(ApprovalStepEntity(reportId = "report", stepIndex = 0, role = "manager",
                thresholdMinor = null, actedBy = "proxy", onBehalfOf = "manager", action = "REJECT", comment = "Duplicate",
                actedAtMillis = 1, claimLineId = "mileage")))
            source.value = updated
            updated
        }
        val session = object : SessionSource {
            override val sessionState = MutableStateFlow(SessionState(kind = SessionKind.CREDENTIALS, employeeCode = "proxy"))
        }
        val vm = PerLineReviewViewModel(reports, session)
        vm.open("report", "mileage")
        advanceUntilIdle()
        assertEquals(route, vm.state.value.route)
        assertFalse(vm.state.value.loading)
        vm.comment("Duplicate")
        vm.act(ApprovalAction.REJECT, "manager")
        advanceUntilIdle()
        coVerify(exactly = 1) { reports.reviewLine("report", 3, "proxy", ApprovalAction.REJECT, "Duplicate", "mileage", "manager") }
        assertEquals(listOf("expense"), vm.state.value.review?.payableLines?.map { it.id })
        assertEquals(ReportLifecycleState.SUBMITTED, vm.state.value.review?.report?.state)
        assertNull(vm.state.value.error)
    }
}
