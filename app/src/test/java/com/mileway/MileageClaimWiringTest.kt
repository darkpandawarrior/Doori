package com.mileway

import com.mileway.core.data.dao.ApprovalStepDao
import com.mileway.core.data.dao.ClaimLineDao
import com.mileway.core.data.dao.ReportDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.dao.VoucherDao
import com.mileway.core.data.di.coreDataModule
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.core.data.model.network.PolicyApprovedVehiclesResponse
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.feature.tracking.claim.AutoDraftOnTripCompleteUseCase
import com.mileway.feature.tracking.di.trackingModule
import com.mileway.feature.tracking.repository.VehiclePricingRepository
import com.mileway.feature.tracking.viewmodel.TrackingSuccessArgs
import com.mileway.feature.tracking.viewmodel.TrackingSuccessViewModel
import com.siddharth.kmp.offlineoutbox.OpOutbox
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Resolves the production Android claim bindings and runs their completion observer and success pricing. */
@OptIn(ExperimentalCoroutinesApi::class)
class MileageClaimWiringTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `production DI auto-drafts the same policy-priced amount shown on success`() =
        runTest {
            val track =
                SavedTrack(
                    routeId = "trip-1",
                    name = "Drive",
                    isCompleted = true,
                    startedByEmployeeCode = "emp-1",
                    startLatitude = 18.0,
                    startLongitude = 73.0,
                    endLatitude = 18.1,
                    endLongitude = 73.1,
                    pausedLatitude = 0.0,
                    pausedLongitude = 0.0,
                    startTime = 1_000L,
                    endTime = 2_000L,
                    distance = 10_000.0,
                    duration = 1_000L,
                    selectedVehicleType = "car",
                )
            val tracks = mockk<SavedTrackDao>()
            val claims = mockk<ClaimLineDao>()
            val outbox = mockk<OpOutbox>()
            val api = mockk<MilewayNetworkApi>()
            val payload = slot<String>()
            coEvery { tracks.getCompletedTracks() } returns flowOf(listOf(track))
            coEvery { claims.getBySourceTripId("trip-1") } returns null
            coEvery { claims.insertMileageDraft(any(), any()) } returns true
            coEvery { outbox.enqueue("report", capture(payload)) } returns "op-1"
            coEvery { api.vehicles(any()) } returns
                PolicyApprovedVehiclesResponse(vehicles = listOf(ApprovedVehicle(vehicleKey = "car", vehiclePricing = 12.0)))
            val storage =
                module {
                    single<SavedTrackDao> { tracks }
                    single<ClaimLineDao> { claims }
                    single<ReportDao> { mockk() }
                    single<ApprovalStepDao> { mockk() }
                    single<OpOutbox> { outbox }
                    single<VoucherDao> { mockk() }
                    single { VehiclePricingRepository(api) }
                }
            val application = koinApplication { modules(coreDataModule, trackingModule, storage) }
            try {
                val koin = application.koin
                koin.get<AutoDraftOnTripCompleteUseCase>().start(backgroundScope)
                val args = TrackingSuccessArgs(10.0, "car", "Car", 1_000L, 2_000L, "txn-1", "SUCCESS", 0, null)
                val success = koin.get<TrackingSuccessViewModel> { parametersOf(args) }
                runCurrent()

                coVerify(exactly = 1) { outbox.enqueue("report", any()) }
                val report = Json.decodeFromString<Report>(payload.captured)
                val line = assertIs<MileageLine>(report.lines.single())
                assertEquals("trip-1", line.sourceTripId)
                assertEquals("emp-1", report.employeeId)
                assertEquals(12_000L, line.amountMinor)
                assertEquals(line.amountMinor / 100.0, success.state.value.reimbursableAmount)
            } finally {
                application.close()
            }
        }
}
