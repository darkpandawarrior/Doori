package com.mileway.core.data.claim

import com.mileway.core.data.dao.ClaimLineDao
import com.mileway.core.data.dao.ReportDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.model.db.ClaimLineEntity
import com.mileway.core.data.model.db.ReportEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * L2: [LegacyMileageBackfillWorker] wraps every completed, non-discarded `saved_tracks` row into a
 * shell report + mileage claim line exactly once (acceptance #3: the `sourceTripId` UNIQUE index is
 * the idempotency anchor — a second [LegacyMileageBackfillWorker.run] or a "late" auto-draft attempt
 * against an already-backfilled trip is a no-op, never a duplicate row), and reads the real legacy
 * `saved_tracks` table (acceptance #4), not any nonexistent entity.
 */
class LegacyMileageBackfillWorkerTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun track(
        routeId: String,
        isCompleted: Boolean = true,
        isDiscarded: Boolean = false,
    ) = SavedTrack(
        routeId = routeId,
        name = "Trip $routeId",
        isCompleted = isCompleted,
        isDiscarded = isDiscarded,
        startedByEmployeeCode = "emp1",
        startLatitude = 0.0,
        startLongitude = 0.0,
        endLatitude = 0.0,
        endLongitude = 0.0,
        pausedLatitude = 0.0,
        pausedLongitude = 0.0,
        startTime = 0L,
        endTime = 0L,
        distance = 10_000.0,
        submittedAmount = 120.0,
        duration = 0L,
        createdAt = 5L,
    )

    private fun worker(
        savedTrackDao: FakeSavedTrackDao,
        reportDao: BackfillFakeReportDao,
        claimLineDao: BackfillFakeClaimLineDao,
        marker: InMemoryBackfillMarker = InMemoryBackfillMarker(),
    ) = LegacyMileageBackfillWorker(savedTrackDao, reportDao, claimLineDao, marker, json)

    @Test
    fun `wraps each completed trip into a shell report and mileage line`() =
        runTest {
            val savedTrackDao = FakeSavedTrackDao(listOf(track("t1"), track("t2")))
            val reportDao = BackfillFakeReportDao()
            val claimLineDao = BackfillFakeClaimLineDao()

            worker(savedTrackDao, reportDao, claimLineDao).run()

            assertEquals(2, reportDao.rows.value.size)
            val line =
                assertIs<MileageLine>(
                    claimLineDao.rows.value
                        .getValue("legacy_line_t1")
                        .toDomain(json),
                )
            assertEquals(10.0, line.distanceKm)
            assertEquals(12_000L, line.amountMinor)
            assertEquals(
                setOf("t1", "t2"),
                claimLineDao.rows.value.values
                    .mapNotNull { it.sourceTripId }
                    .toSet(),
            )
        }

    @Test
    fun `failed pass leaves marker unset and retries without duplicating completed imports`() =
        runTest {
            val savedTrackDao = FakeSavedTrackDao(listOf(track("t1"), track("t2")))
            val reportDao = BackfillFakeReportDao()
            val claimLineDao = BackfillFakeClaimLineDao()
            val marker = InMemoryBackfillMarker()
            val backfill = worker(savedTrackDao, reportDao, claimLineDao, marker)
            claimLineDao.failOnSourceTripId = "t2"

            assertFailsWith<IllegalStateException> { backfill.run() }
            assertFalse(marker.isDone())
            assertEquals(1, claimLineDao.rows.value.size)

            claimLineDao.failOnSourceTripId = null
            backfill.run()
            assertTrue(marker.isDone())
            assertEquals(2, reportDao.rows.value.size)
            assertEquals(2, claimLineDao.rows.value.size)
        }

    @Test
    fun `discarded trips are not backfilled`() =
        runTest {
            val savedTrackDao = FakeSavedTrackDao(listOf(track("t1", isDiscarded = true)))
            val claimLineDao = BackfillFakeClaimLineDao()

            worker(savedTrackDao, BackfillFakeReportDao(), claimLineDao).run()

            assertTrue(claimLineDao.rows.value.isEmpty())
        }

    @Test
    fun `re-running after the marker is set is a no-op`() =
        runTest {
            val savedTrackDao = FakeSavedTrackDao(listOf(track("t1")))
            val claimLineDao = BackfillFakeClaimLineDao()
            val marker = InMemoryBackfillMarker()

            worker(savedTrackDao, BackfillFakeReportDao(), claimLineDao, marker).run()
            assertEquals(1, claimLineDao.rows.value.size)

            savedTrackDao.rows.value = savedTrackDao.rows.value + track("t2")
            worker(savedTrackDao, BackfillFakeReportDao(), claimLineDao, marker).run()

            // marker already set by the first run — t2 is never picked up by this second call.
            assertEquals(1, claimLineDao.rows.value.size)
        }

    @Test
    fun `a trip already claimed by sourceTripId is skipped even without the marker`() =
        runTest {
            val savedTrackDao = FakeSavedTrackDao(listOf(track("t1")))
            val claimLineDao = BackfillFakeClaimLineDao()
            claimLineDao.rows.value =
                mapOf(
                    "existing" to
                        ClaimLineEntity(
                            id = "existing",
                            reportId = "r_existing",
                            type = "mileage",
                            amountMinor = 0,
                            currency = "INR",
                            fxRatePinnedAt = null,
                            policyFlagsCsv = "",
                            cardMatchId = null,
                            sourceTripId = "t1",
                            detailsJson = "{}",
                            createdAtMs = 0L,
                        ),
                )

            // Marker never set (isDone() = false), so run() attempts every completed track again —
            // this is the "L4 auto-draft already claimed it first" idempotency case, not the
            // marker-gated repeat-run case above.
            worker(savedTrackDao, BackfillFakeReportDao(), claimLineDao).run()

            assertEquals(1, claimLineDao.rows.value.size)
        }
}

private class InMemoryBackfillMarker : BackfillMarker {
    private var done = false

    override suspend fun isDone(): Boolean = done

    override suspend fun markDone() {
        done = true
    }
}

private class FakeSavedTrackDao(
    initial: List<SavedTrack>,
) : SavedTrackDao {
    val rows = MutableStateFlow(initial)

    override suspend fun insertSavedTrack(savedTrack: SavedTrack) {
        rows.value = rows.value + savedTrack
    }

    override suspend fun updateSavedTrack(savedTrack: SavedTrack): Int = 0

    override suspend fun deleteSavedTrack(track: SavedTrack) = Unit

    override suspend fun deleteSavedTrack(routeId: String) = Unit

    override suspend fun deleteTracksByAccount(employeeCode: String): Int = 0

    override fun getAllSavedTracks(): Flow<List<SavedTrack>> = rows

    override fun getAllSavedTracksByAccount(accountId: String): Flow<List<SavedTrack>> = rows

    override fun getCompletedTracks(): Flow<List<SavedTrack>> = rows.map { list -> list.filter { it.isCompleted } }

    override suspend fun count(): Long = rows.value.size.toLong()

    override suspend fun getActiveTrack(): SavedTrack? = null

    override suspend fun getActiveTrackByAccount(employeeCode: String): SavedTrack? = null

    override fun getPausedTracksByAccount(employeeCode: String): Flow<List<SavedTrack>> = rows

    override suspend fun getMostRecentActiveTrack(): SavedTrack? = null

    override suspend fun getLastCompletedTrack(): SavedTrack? = null

    override suspend fun getSavedTrackById(routeId: String): SavedTrack? = rows.value.firstOrNull { it.routeId == routeId }

    override fun observeTrackById(routeId: String): Flow<SavedTrack?> = rows.map { list -> list.firstOrNull { it.routeId == routeId } }

    override fun getRetainedTracks(): Flow<List<SavedTrack>> = rows

    override fun getTracksInRange(
        start: Long,
        end: Long,
    ): Flow<List<SavedTrack>> = rows

    override fun getTracksInRangeExcludingRetained(
        start: Long,
        end: Long,
    ): Flow<List<SavedTrack>> = rows

    override suspend fun countInRangeExcludingRetained(
        start: Long,
        end: Long,
    ): Int = 0

    override suspend fun updateTrackName(
        routeId: String,
        name: String,
    ) = Unit

    override suspend fun updateSmartDistanceFinal(
        routeId: String,
        value: Double,
    ) = Unit

    override suspend fun updateTrackLiveData(
        routeId: String,
        distance: Double,
        duration: Long,
    ) = Unit

    override suspend fun markTrackDraft(
        routeId: String,
        draftSavedAt: Long,
    ): Int = 0

    override suspend fun updateSubmissionTime(
        routeId: String,
        submissionTime: Long,
    ): Int = 0

    override suspend fun finalizeTrack(
        routeId: String,
        endTime: Long,
        finalDistance: Double,
        avgSpeed: Double,
        maxSpeed: Double,
    ) = Unit

    override suspend fun markTrackCompleted(
        routeId: String,
        trackingActivity: String,
        currentTime: Long,
        newName: String,
        submittedAmount: Double,
        submittedAmountCurrency: String,
        transId: String?,
    ): Int = 0

    override suspend fun markTrackEndedLocally(
        routeId: String,
        trackingActivity: String,
        currentTime: Long,
        newName: String,
    ): Int = 0

    override suspend fun markRetained(routeIds: List<String>) = Unit

    override suspend fun markRetainedBefore(threshold: Long): Int = 0

    override suspend fun setRetained(
        routeId: String,
        retained: Boolean,
    ) = Unit

    override suspend fun deleteCorruptedTracks(): Int = 0

    override suspend fun getCorruptedTrackCount(): Int = 0

    override suspend fun deleteOlderThanExcludingRetained(threshold: Long): Int = 0

    override suspend fun getLastNRouteIdsFromRange(
        start: Long,
        end: Long,
        limit: Int,
    ): List<String> = emptyList()

    override suspend fun getAverageTrackMetrics(): TrackMetrics = TrackMetrics(0.0, 0L, 0f, 0)

    override suspend fun getPreviousSimilarTrack(routeId: String): SavedTrack? = null

    override suspend fun getSimilarTracks(routeId: String): List<SavedTrack> = emptyList()

    override suspend fun getRouteIdsEligibleForCleanup(cutoffMillis: Long): List<String> = emptyList()

    override suspend fun markLocalDataPurged(routeId: String) = Unit

    override suspend fun markAppKilled(routeId: String): Int = 0

    override suspend fun markFgTerminated(routeId: String): Int = 0

    override suspend fun markPhoneShutDown(routeId: String): Int = 0

    override suspend fun markClaimedByVoucher(
        routeId: String,
        voucherNumber: String,
    ): Int = 0

    override suspend fun markOdometerNotWorking(routeId: String): Int = 0

    override suspend fun setOfficeAndEntity(
        routeId: String,
        officeId: Long?,
        entityId: Long?,
    ): Int = 0
}

private class BackfillFakeReportDao : ReportDao {
    val rows = MutableStateFlow<Map<String, ReportEntity>>(emptyMap())

    override suspend fun get(id: String): ReportEntity? = rows.value[id]

    override fun observe(id: String): Flow<ReportEntity?> = rows.map { it[id] }

    override fun observeByEmployee(employeeId: String): Flow<List<ReportEntity>> = rows.map { it.values.toList() }

    override fun observeAll(): Flow<List<ReportEntity>> = rows.map { it.values.toList() }

    override suspend fun awaitingPayment(): List<ReportEntity> =
        rows.value.values.filter {
            it.state in setOf("APPROVED", "APPROVED_FOR_PAYMENT")
        }

    override suspend fun upsert(entity: ReportEntity) {
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }
}

private class BackfillFakeClaimLineDao : ClaimLineDao {
    val rows = MutableStateFlow<Map<String, ClaimLineEntity>>(emptyMap())
    var failOnSourceTripId: String? = null

    override fun observeByReport(reportId: String): Flow<List<ClaimLineEntity>> = rows.map { it.values.filter { row -> row.reportId == reportId } }

    override suspend fun getByReport(reportId: String): List<ClaimLineEntity> = rows.value.values.filter { it.reportId == reportId }

    override suspend fun getBySourceTripId(sourceTripId: String): ClaimLineEntity? = rows.value.values.firstOrNull { it.sourceTripId == sourceTripId }

    override suspend fun insert(entity: ClaimLineEntity) {
        check(entity.sourceTripId != failOnSourceTripId) { "storage unavailable" }
        check(entity.sourceTripId == null || rows.value.values.none { it.sourceTripId == entity.sourceTripId }) {
            "UNIQUE constraint failed: claim_lines.sourceTripId"
        }
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun insertIfAbsent(entity: ClaimLineEntity): Long {
        if (rows.value.containsKey(entity.id) || (entity.sourceTripId != null && getBySourceTripId(entity.sourceTripId) != null)) return -1L
        insert(entity)
        return 1L
    }

    override suspend fun insertDraftReport(entity: ReportEntity) = error("unused in this fake")

    override suspend fun upsert(entity: ClaimLineEntity) {
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }
}
