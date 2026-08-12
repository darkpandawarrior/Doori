package com.mileway.feature.tracking.viewmodel

import com.mileway.core.data.dao.TripAttachmentDao
import com.mileway.core.data.model.db.AttachmentType
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TripAttachmentEntity
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.repository.TripAttachmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The three record-editing actions added to [TrackDetailViewModel]: edit distance, toggle
 * personal/business, and discard. These write straight to the reimbursement amount and to
 * what a journey looks like once submitted, so they get one runnable check each rather than
 * being trusted on read-through alone.
 */
class TrackDetailViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun track(routeId: String = "r1") =
        SavedTrack(
            routeId = routeId,
            name = "Journey $routeId",
            startLatitude = 0.0, startLongitude = 0.0,
            endLatitude = 0.0, endLongitude = 0.0,
            pausedLatitude = 0.0, pausedLongitude = 0.0,
            startTime = 1_000L, endTime = 2_000L,
            distance = 4_200.0, duration = 1_000L,
            startedAtTimestamp = 1_000L,
        )

    private fun viewModel(dao: FakeSavedTrackDao) =
        TrackDetailViewModel(
            trackRepository = SavedTrackRepository(dao),
            locationRepository = LocationRepository(FakeLocationDao()),
            attachmentRepository = TripAttachmentRepository(StubTripAttachmentDao()),
        )

    @Test
    fun `edit distance persists the corrected km`() =
        runTest {
            val dao = FakeSavedTrackDao(listOf(track()))
            val vm = viewModel(dao)
            vm.onAction(TrackDetailAction.Load("r1"))

            vm.onAction(TrackDetailAction.EditDistance(12.5))

            assertEquals(12.5, vm.state.value.track?.distanceKm)
            assertEquals(12_500.0, dao.getSavedTrackById("r1")?.distance)
        }

    @Test
    fun `edit distance rejects zero or negative and leaves the record untouched`() =
        runTest {
            val dao = FakeSavedTrackDao(listOf(track()))
            val vm = viewModel(dao)
            vm.onAction(TrackDetailAction.Load("r1"))

            vm.onAction(TrackDetailAction.EditDistance(0.0))

            assertEquals(4_200.0, dao.getSavedTrackById("r1")?.distance)
        }

    @Test
    fun `toggle personal flips the classification tag and back`() =
        runTest {
            val dao = FakeSavedTrackDao(listOf(track()))
            val vm = viewModel(dao)
            vm.onAction(TrackDetailAction.Load("r1"))

            vm.onAction(TrackDetailAction.TogglePersonal)
            assertEquals("PERSONAL", dao.getSavedTrackById("r1")?.notes)

            vm.onAction(TrackDetailAction.TogglePersonal)
            assertEquals("-", dao.getSavedTrackById("r1")?.notes)
        }

    @Test
    fun `discard removes the row and emits Discarded`() =
        runTest {
            val dao = FakeSavedTrackDao(listOf(track()))
            val vm = viewModel(dao)
            vm.onAction(TrackDetailAction.Load("r1"))

            vm.onAction(TrackDetailAction.Discard)

            assertEquals(TrackDetailEffect.Discarded, vm.effect.first())
            assertNull(dao.getSavedTrackById("r1"))
        }

    @Test
    fun `discarding an already-removed journey reports a failure instead of a crash`() =
        runTest {
            val dao = FakeSavedTrackDao(listOf(track()))
            val vm = viewModel(dao)
            vm.onAction(TrackDetailAction.Load("r1"))
            dao.deleteSavedTrack("r1") // race: removed elsewhere between load and the tap

            vm.onAction(TrackDetailAction.Discard)

            assertEquals(true, vm.effect.first() is TrackDetailEffect.ActionFailed)
        }
}

/** Minimal no-op fake — [TrackDetailViewModel] only ever observes an empty attachment stream here. */
private class StubTripAttachmentDao : TripAttachmentDao {
    override suspend fun insert(attachment: TripAttachmentEntity): Long = 0L

    override fun observeForTrack(trackToken: String): Flow<List<TripAttachmentEntity>> = flowOf(emptyList())

    override suspend fun getForTrack(trackToken: String): List<TripAttachmentEntity> = emptyList()

    override suspend fun getLatestOfType(
        trackToken: String,
        type: AttachmentType,
    ): TripAttachmentEntity? = null

    override fun observeByType(
        trackToken: String,
        type: AttachmentType,
    ): Flow<List<TripAttachmentEntity>> = flowOf(emptyList())

    override suspend fun delete(id: Long) = Unit

    override suspend fun deleteForTrack(trackToken: String) = Unit

    override suspend fun countForTrack(trackToken: String): Int = 0
}
