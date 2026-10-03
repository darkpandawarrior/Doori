package com.mileway.feature.tracking.claim

import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.dao.ApprovalStepDao
import com.mileway.core.data.dao.ClaimLineDao
import com.mileway.core.data.dao.ReportDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.ApprovalStepEntity
import com.mileway.core.data.model.db.ClaimLineEntity
import com.mileway.core.data.model.db.ReportEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.feature.tracking.repository.VehiclePricingRepository
import com.mileway.feature.tracking.viewmodel.FakeNetworkApi
import com.mileway.feature.tracking.viewmodel.FakeSavedTrackDao
import com.siddharth.kmp.offlineoutbox.OpEntry
import com.siddharth.kmp.offlineoutbox.OpOutbox
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AutoDraftOnTripCompleteUseCaseTest {
    private val reports = TestReportDao()
    private val claims = TestClaimDao(reports)
    private val queued = mutableListOf<String>()
    private val repository = ReportRepository(reports, claims, TestApprovalDao(), TestOutbox(queued), Json)
    private val completed = MutableStateFlow<List<SavedTrack>>(emptyList())
    private val tracks =
        object : SavedTrackDao by FakeSavedTrackDao() {
            override fun getCompletedTracks(): Flow<List<SavedTrack>> = completed
        }
    private val policy =
        MileageClaimPolicyProvider(
            VehiclePricingRepository(FakeNetworkApi(listOf(ApprovedVehicle(vehicleKey = "car", vehiclePricing = 12.0)))),
        )
    private val useCase = AutoDraftOnTripCompleteUseCase(tracks, repository, policy)

    @Test
    fun `persisted completion drafts one priced report and repeat emissions cannot change it`() =
        runTest {
            useCase.start(backgroundScope)
            useCase.start(backgroundScope)
            completed.value = listOf(completedTrip())
            runCurrent()

            val report = requireNotNull(repository.get("mileage_trip-1"))
            assertEquals("emp-1", report.employeeId)
            assertEquals(ReportLifecycleState.DRAFT, report.state)
            assertEquals(1L, report.recordVersion)
            val line = assertIs<MileageLine>(report.lines.single())
            assertEquals(12_000L, line.amountMinor)
            assertEquals("trip-1", line.sourceTripId)
            assertEquals(1, queued.size)
            assertEquals(report, Json.decodeFromString<Report>(queued.single()))

            completed.value = listOf(completedTrip().copy(distance = 20_000.0))
            runCurrent()
            assertEquals(report, repository.get(report.id))
            assertEquals(1, claims.rows.size)
            assertEquals(1, queued.size)
        }

    @Test
    fun `legacy source trip claim remains untouched and no draft is created`() =
        runTest {
            val legacy =
                ClaimLineEntity("legacy_line_trip-1", "legacy_trip-1", "mileage", 5_000L, "INR", null, "", null, "trip-1", "{}", 0L)
            claims.insert(legacy)
            assertNull(useCase(completedTrip()))
            assertEquals(legacy, claims.rows.values.single())
            assertEquals(emptyMap(), reports.rows)
            assertEquals(emptyList(), queued)
        }

    @Test
    fun `startup recovers a missed completion and ineligible rows stay unclaimed`() =
        runTest {
            completed.value = listOf(completedTrip(), completedTrip().copy(routeId = "mock", wasMockOn = true))
            useCase.start(backgroundScope)
            runCurrent()
            assertEquals(1, reports.rows.size)
            assertEquals(1, claims.rows.size)
            assertNull(claims.getBySourceTripId("mock"))
        }
}

private class TestReportDao : ReportDao {
    val rows = mutableMapOf<String, ReportEntity>()

    override suspend fun get(id: String): ReportEntity? = rows[id]

    override fun observe(id: String): Flow<ReportEntity?> = flowOf(rows[id])

    override fun observeByEmployee(employeeId: String): Flow<List<ReportEntity>> = flowOf(rows.values.filter { it.employeeId == employeeId })

    override suspend fun upsert(entity: ReportEntity) {
        rows[entity.id] = entity
    }

    override suspend fun delete(id: String) {
        rows.remove(id)
    }
}

private class TestClaimDao(
    private val reports: TestReportDao,
) : ClaimLineDao {
    val rows = mutableMapOf<String, ClaimLineEntity>()

    override fun observeByReport(reportId: String): Flow<List<ClaimLineEntity>> = flowOf(rows.values.filter { it.reportId == reportId })

    override suspend fun getByReport(reportId: String): List<ClaimLineEntity> = rows.values.filter { it.reportId == reportId }

    override suspend fun getBySourceTripId(sourceTripId: String): ClaimLineEntity? = rows.values.firstOrNull { it.sourceTripId == sourceTripId }

    override suspend fun insert(entity: ClaimLineEntity) {
        check(insertIfAbsent(entity) != -1L)
    }

    override suspend fun insertIfAbsent(entity: ClaimLineEntity): Long {
        if (rows.containsKey(entity.id) || rows.values.any { it.sourceTripId == entity.sourceTripId }) return -1L
        rows[entity.id] = entity
        return 1L
    }

    override suspend fun insertDraftReport(entity: ReportEntity) {
        check(!reports.rows.containsKey(entity.id))
        reports.upsert(entity)
    }

    override suspend fun upsert(entity: ClaimLineEntity) {
        rows[entity.id] = entity
    }

    override suspend fun delete(id: String) {
        rows.remove(id)
    }
}

private class TestApprovalDao : ApprovalStepDao {
    override fun observeByReport(reportId: String): Flow<List<ApprovalStepEntity>> = flowOf(emptyList())

    override suspend fun getByReport(reportId: String): List<ApprovalStepEntity> = emptyList()

    override suspend fun insert(entity: ApprovalStepEntity) = Unit

    override suspend fun deleteByReport(reportId: String) = Unit
}

private class TestOutbox(
    private val queued: MutableList<String>,
) : OpOutbox {
    override suspend fun enqueue(
        type: String,
        payload: String,
    ): String {
        check(type == "report")
        queued += payload
        return queued.size.toString()
    }

    override fun pending(): Flow<List<OpEntry>> = flowOf(emptyList())

    override suspend fun deadLetters(): List<OpEntry> = emptyList()

    override suspend fun replay(
        maxAttempts: Int,
        isPermanent: (Throwable) -> Boolean,
        send: suspend (OpEntry) -> Unit,
    ) = Unit

    override suspend fun requeue(id: String) = Unit
}
