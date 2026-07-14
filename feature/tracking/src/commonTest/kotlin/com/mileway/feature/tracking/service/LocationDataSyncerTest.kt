package com.mileway.feature.tracking.service

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.outbox.LocationBatch
import com.mileway.core.data.outbox.LocationBatchOutbox
import com.siddharth.kmp.offlineoutbox.DraftEntry
import com.siddharth.kmp.offlineoutbox.DraftStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Wave-4 §2.3: [LocationDataSyncer] policy — batch caps, min sync gap, permanent-drop handling,
 * and [SyncStatus] transitions.
 */
class LocationDataSyncerTest {
    private fun point(id: Long) = LocationData(id = id, activity = "", speed = 0f, lat = 0.0, lng = 0.0, token = "t", batteryPercentage = 100.0)

    @Test
    fun `a single call never sends more than MAX_POINTS_PER_CALL points in one batch`() =
        runTest {
            val dao = FakeSyncLocationDao(unsynced = (1..500L).map { point(it) })
            val outbox = FakeLocationBatchOutbox()
            val syncer = LocationDataSyncer(dao, outbox, now = { 0L })

            syncer.drain("t")

            assertTrue(outbox.enqueued.all { it.pointIds.size <= LocationDataSyncer.MAX_POINTS_PER_CALL })
        }

    @Test
    fun `a single drain never sends more than MAX_BATCHES_PER_DRAIN batches`() =
        runTest {
            // 200 pts / 120 per batch = 2 batches worth; use a huge unsynced pool to prove the cap bites.
            val dao = FakeSyncLocationDao(unsynced = (1..10_000L).map { point(it) })
            val outbox = FakeLocationBatchOutbox()
            val syncer = LocationDataSyncer(dao, outbox, now = { 0L })

            syncer.drain("t")

            assertTrue(outbox.enqueued.size <= LocationDataSyncer.MAX_BATCHES_PER_DRAIN)
        }

    @Test
    fun `a second drain within the min gap is a no-op`() =
        runTest {
            val dao = FakeSyncLocationDao(unsynced = (1..5L).map { point(it) })
            val outbox = FakeLocationBatchOutbox()
            var clock = 0L
            val syncer = LocationDataSyncer(dao, outbox, now = { clock })

            syncer.drain("t")
            val afterFirst = outbox.enqueued.size
            clock += LocationDataSyncer.MIN_SYNC_GAP_MS - 1
            syncer.drain("t")

            assertEquals(afterFirst, outbox.enqueued.size, "drain called inside the min gap should not enqueue again")
        }

    @Test
    fun `a drain after the min gap elapses is allowed to run again`() =
        runTest {
            val dao = FakeSyncLocationDao(unsynced = (1..5L).map { point(it) })
            val outbox = FakeLocationBatchOutbox()
            var clock = 0L
            val syncer = LocationDataSyncer(dao, outbox, now = { clock })

            syncer.drain("t")
            clock += LocationDataSyncer.MIN_SYNC_GAP_MS
            dao.unsynced.addAll((6..10L).map { point(it) })
            syncer.drain("t")

            assertEquals(2, outbox.enqueued.size)
        }

    @Test
    fun `a permanent failure (409-5xx-equivalent) drops the batch instead of retrying it`() =
        runTest {
            val dao = FakeSyncLocationDao(unsynced = (1..3L).map { point(it) })
            val outbox = FakeLocationBatchOutbox()
            val syncer = LocationDataSyncer(dao, outbox, now = { 0L }, send = { SendOutcome.PERMANENT_FAILURE })

            syncer.drain("t")

            assertEquals(DraftStatus.FAILED, outbox.enqueued.single().let { outbox.statusFor(it) })
            assertTrue(dao.markedSynced.isEmpty(), "a permanently-failed batch must not be marked synced")
        }

    @Test
    fun `syncStatus transitions Idle to Syncing to Synced with the resulting backlog`() =
        runTest {
            val dao = FakeSyncLocationDao(unsynced = (1..3L).map { point(it) })
            val outbox = FakeLocationBatchOutbox()
            val syncer = LocationDataSyncer(dao, outbox, now = { 42L })

            assertIs<SyncStatus.Idle>(syncer.syncStatus.value)
            syncer.drain("t")
            val finalStatus = syncer.syncStatus.value
            assertIs<SyncStatus.Synced>(finalStatus)
            assertEquals(42L, finalStatus.lastSyncedAtMs)
            assertEquals(0, finalStatus.backlogCount)
        }
}

private class FakeSyncLocationDao(unsynced: List<LocationData>) : LocationDao {
    val unsynced = unsynced.toMutableList()
    val markedSynced = mutableListOf<Long>()

    override fun getLocationsByToken(token: String): Flow<List<LocationData>> = flowOf(emptyList())

    override suspend fun getLocationsByTokenOnce(token: String): List<LocationData> = emptyList()

    override suspend fun getLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData> = emptyList()

    override suspend fun countLocationsByToken(token: String): Int = 0

    override fun getAllLocations(): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getLocationsByUploadStatus(uploaded: Boolean): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getLocationsByActivity(activity: String): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getLocationsByDateRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getCheckInLocationsByToken(token: String): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getAllCheckInPoints(): Flow<List<LocationData>> = flowOf(emptyList())

    override suspend fun insertLocation(location: LocationData) {}

    override suspend fun insertLocations(locations: List<LocationData>) {}

    override suspend fun updateLocation(location: LocationData) {}

    override suspend fun updateUploadStatus(
        id: Long,
        uploaded: Boolean,
    ) {}

    override suspend fun updateUploadStatusByToken(
        token: String,
        uploaded: Boolean,
    ) {}

    override suspend fun deleteLocation(location: LocationData) {}

    override suspend fun deleteLocationById(id: Long) {}

    override suspend fun deleteLocationsByToken(token: String) {}

    override suspend fun deleteUploadedLocations(uploadedValue: Boolean) {}

    override suspend fun deleteAllLocations() {}

    override suspend fun getLocationCount(): Int = unsynced.size

    override suspend fun getUnuploadedLocationCount(uploadedValue: Boolean): Int = unsynced.size

    override suspend fun getUnsyncedLocationsByToken(token: String): List<LocationData> = unsynced.toList()

    override suspend fun getUnsyncedLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData> = unsynced.drop(offset).take(limit)

    override suspend fun markLocationsAsSynced(locationIds: List<Long>) {
        markedSynced.addAll(locationIds)
        unsynced.removeAll { it.id in locationIds }
    }

    override suspend fun deleteOlderThan(timestamp: Long): Int = 0

    override suspend fun getFirstUnsyncedLocationByToken(token: String): LocationData? = unsynced.firstOrNull()

    override suspend fun getLastLocationByToken(token: String): LocationData? = unsynced.lastOrNull()
}

/** In-memory [LocationBatchOutbox] — mirrors FakeSubmitOutbox in LogMilesSubmitUseCaseTest. */
private class FakeLocationBatchOutbox : LocationBatchOutbox {
    private val entries = MutableStateFlow<Map<String, DraftEntry<LocationBatch>>>(emptyMap())

    val enqueued: List<LocationBatch> get() = entries.value.values.map { it.payload }

    fun statusFor(batch: LocationBatch): DraftStatus = entries.value.values.first { it.payload == batch }.status

    override fun drafts(formKey: String): Flow<List<DraftEntry<LocationBatch>>> = entries.map { it.values.filter { e -> e.formKey == formKey } }

    override suspend fun enqueue(
        formKey: String,
        uniqueKey: String,
        payload: LocationBatch,
    ) {
        entries.value = entries.value + (uniqueKey to DraftEntry(formKey, uniqueKey, payload, DraftStatus.PENDING, null, 0L, 0L))
    }

    override suspend fun markSubmitted(
        formKey: String,
        uniqueKey: String,
    ) {
        entries.value = entries.value + (uniqueKey to entries.value.getValue(uniqueKey).copy(status = DraftStatus.SUBMITTED))
    }

    override suspend fun markFailed(
        formKey: String,
        uniqueKey: String,
        error: String,
    ) {
        entries.value =
            entries.value + (uniqueKey to entries.value.getValue(uniqueKey).copy(status = DraftStatus.FAILED, errorMessage = error))
    }

    override suspend fun clear(formKey: String) {
        entries.value = entries.value.filterValues { it.formKey != formKey }
    }
}
