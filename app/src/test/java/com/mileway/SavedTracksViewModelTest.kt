package com.mileway

import com.mileway.core.data.model.db.SavedTrack
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.viewmodel.JourneyFilter
import com.mileway.feature.tracking.viewmodel.SavedTracksAction
import com.mileway.feature.tracking.viewmodel.SavedTracksTab
import com.mileway.feature.tracking.viewmodel.SavedTracksViewModel
import com.mileway.feature.tracking.viewmodel.SubmissionFilter
import com.mileway.feature.tracking.viewmodel.SubmissionSource
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SavedTracksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeSavedTrackDao()
    private val repo = SavedTrackRepository(dao)

    private fun viewModel() = SavedTracksViewModel(repo)

    /** Creates a completed [SavedTrack] optionally marked as submitted to the server. */
    private fun makeTrack(
        routeId: String,
        submitted: Boolean = false,
        amount: Double = 10.0,
    ) = SavedTrack(
        routeId = routeId,
        name = "Test journey $routeId",
        isCompleted = true,
        serverUploaded = submitted,
        submittedAmount = amount,
        submissionTime = if (submitted) 1L else 0L,
        startLatitude = 0.0, startLongitude = 0.0,
        endLatitude = 0.0, endLongitude = 0.0,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = 0L, endTime = 1L,
        distance = 5_000.0, duration = 60_000L,
    )

    @Test
    fun `init loads tracks from repository`() = runTest {
        dao.preload(makeTrack("T1"))
        dao.preload(makeTrack("T2"))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.tracks.size)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `TabSelected switches active tab and clears selection`() = runTest {
        dao.preload(makeTrack("T1", submitted = true))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(SavedTracksAction.SubmissionLongPressed("T1"))
        assertTrue(vm.state.value.selectionMode)

        vm.onAction(SavedTracksAction.TabSelected(SavedTracksTab.SUBMISSIONS))
        assertEquals(SavedTracksTab.SUBMISSIONS, vm.state.value.tab)
        assertFalse(vm.state.value.selectionMode)
        assertTrue(vm.state.value.selectedSubmissionIds.isEmpty())
    }

    @Test
    fun `JourneySearchChanged updates journeySearch`() = runTest {
        val vm = viewModel()
        vm.onAction(SavedTracksAction.JourneySearchChanged("weekly"))
        assertEquals("weekly", vm.state.value.journeySearch)
    }

    @Test
    fun `JourneyFilterSelected updates active chip`() = runTest {
        val vm = viewModel()
        vm.onAction(SavedTracksAction.JourneyFilterSelected(JourneyFilter.ALL))
        assertEquals(JourneyFilter.ALL, vm.state.value.journeyFilter)
        vm.onAction(SavedTracksAction.JourneyFilterSelected(JourneyFilter.KEPT))
        assertEquals(JourneyFilter.KEPT, vm.state.value.journeyFilter)
    }

    @Test
    fun `SubmissionFilterSelected and SubmissionSourceSelected update chips`() = runTest {
        val vm = viewModel()
        vm.onAction(SavedTracksAction.SubmissionFilterSelected(SubmissionFilter.UNCLAIMED))
        assertEquals(SubmissionFilter.UNCLAIMED, vm.state.value.submissionFilter)
        vm.onAction(SavedTracksAction.SubmissionSourceSelected(SubmissionSource.NEW_TRACKER))
        assertEquals(SubmissionSource.NEW_TRACKER, vm.state.value.submissionSource)
    }

    @Test
    fun `SubmissionLongPressed enters selection mode with the id`() = runTest {
        dao.preload(makeTrack("T1", submitted = true))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(SavedTracksAction.SubmissionLongPressed("T1"))
        assertTrue(vm.state.value.selectionMode)
        assertTrue("T1" in vm.state.value.selectedSubmissionIds)
    }

    @Test
    fun `SubmissionSelectionToggled deselects when already selected`() = runTest {
        dao.preload(makeTrack("T1", submitted = true))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(SavedTracksAction.SubmissionLongPressed("T1"))
        assertTrue("T1" in vm.state.value.selectedSubmissionIds)

        vm.onAction(SavedTracksAction.SubmissionSelectionToggled("T1"))
        assertFalse("T1" in vm.state.value.selectedSubmissionIds)
        assertFalse(vm.state.value.selectionMode)
    }

    @Test
    fun `ClearSelection exits selection mode`() = runTest {
        dao.preload(makeTrack("T1", submitted = true))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(SavedTracksAction.SubmissionLongPressed("T1"))
        vm.onAction(SavedTracksAction.ClearSelection)
        assertFalse(vm.state.value.selectionMode)
        assertTrue(vm.state.value.selectedSubmissionIds.isEmpty())
    }

    @Test
    fun `CreateVoucher emits ack flag and clears selection`() = runTest {
        dao.preload(makeTrack("T1", submitted = true))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(SavedTracksAction.SubmissionLongPressed("T1"))
        vm.onAction(SavedTracksAction.CreateVoucher)

        assertTrue(vm.state.value.voucherCreatedAck)
        assertFalse(vm.state.value.selectionMode)
    }

    @Test
    fun `VoucherAckConsumed clears the ack flag`() = runTest {
        dao.preload(makeTrack("T1", submitted = true))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(SavedTracksAction.SubmissionLongPressed("T1"))
        vm.onAction(SavedTracksAction.CreateVoucher)
        assertTrue(vm.state.value.voucherCreatedAck)

        vm.onAction(SavedTracksAction.VoucherAckConsumed)
        assertFalse(vm.state.value.voucherCreatedAck)
    }

    @Test
    fun `selection is pruned when a submitted track is removed from repository`() = runTest {
        dao.preload(makeTrack("T1", submitted = true))
        dao.preload(makeTrack("T2", submitted = true))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(SavedTracksAction.SubmissionLongPressed("T1"))
        vm.onAction(SavedTracksAction.SubmissionSelectionToggled("T2"))
        assertEquals(setOf("T1", "T2"), vm.state.value.selectedSubmissionIds)

        // Remove T1 from the repository, allTracksFlow re-emits, selection is pruned
        dao.removeTrack("T1")
        advanceUntilIdle()
        assertFalse("T1" in vm.state.value.selectedSubmissionIds)
        assertTrue("T2" in vm.state.value.selectedSubmissionIds)
    }
}
