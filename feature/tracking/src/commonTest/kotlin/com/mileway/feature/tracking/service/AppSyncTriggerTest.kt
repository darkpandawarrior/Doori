package com.mileway.feature.tracking.service

import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.LocationData
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V34 P1: [AppSyncTrigger]'s app-scoped triggers — the offline→online edge collector (with
 * its skip-the-startup-emission guard) and the explicit foreground drain. The drain internals are
 * [LocationDataSyncerTest]/[MilesSubmitSyncerTest]'s job; here we only assert the triggers fire.
 */
class AppSyncTriggerTest {
    private fun point(id: Long) = LocationData(id = id, activity = "", speed = 0f, lat = 0.0, lng = 0.0, token = "t", batteryPercentage = 100.0)

    // The trigger scope shares the test scheduler but owns an independent Job: start()'s
    // connectivity collector never completes, so a plain child scope would hang runTest (and this
    // coroutines-test version doesn't advance backgroundScope jobs from advanceUntilIdle).
    private fun TestScope.triggerScope(): CoroutineScope = CoroutineScope(coroutineContext + Job())

    private fun harness(
        scope: CoroutineScope,
        connectivity: MutableStateFlow<Boolean>,
    ): Pair<AppSyncTrigger, FakeLocationBatchOutbox> {
        val outbox = FakeLocationBatchOutbox()
        val syncer =
            LocationDataSyncer(
                locationDao = FakeSyncLocationDao(unsynced = (1..3L).map { point(it) }),
                outbox = outbox,
                now = { 0L },
            )
        val milesSyncer =
            MilesSubmitSyncer(
                outbox = FakeTripDraftOutbox(),
                trackRepository = SavedTrackRepository(FakeMilesSubmitDao()),
                now = { 0L },
            )
        val trigger =
            AppSyncTrigger(
                syncer = syncer,
                milesSyncer = milesSyncer,
                currentTrackRepo = CurrentTrackRepository(FakeCurrentTrackSource(CurrentTrackData(token = "t"))),
                isConnectedFlow = connectivity,
                scope = scope,
            )
        return trigger to outbox
    }

    @Test
    fun `onAppForeground drains the location outbox`() =
        runTest {
            val scope = triggerScope()
            val (trigger, outbox) = harness(scope, MutableStateFlow(true))

            trigger.onAppForeground()
            advanceUntilIdle()

            assertTrue(outbox.enqueued.isNotEmpty(), "foreground trigger should have drained the pending points")
            scope.cancel()
        }

    @Test
    fun `startup connectivity emission does not trigger a drain`() =
        runTest {
            val scope = triggerScope()
            val connectivity = MutableStateFlow(true)
            val (trigger, outbox) = harness(scope, connectivity)

            trigger.start()
            advanceUntilIdle()

            assertEquals(0, outbox.enqueued.size, "the initial already-online emission must not drain")
            scope.cancel()
        }

    @Test
    fun `offline to online edge triggers a drain`() =
        runTest {
            val scope = triggerScope()
            val connectivity = MutableStateFlow(true)
            val (trigger, outbox) = harness(scope, connectivity)

            trigger.start()
            advanceUntilIdle()
            connectivity.value = false
            advanceUntilIdle()
            connectivity.value = true
            advanceUntilIdle()

            assertTrue(outbox.enqueued.isNotEmpty(), "offline→online edge should have drained the pending points")
            scope.cancel()
        }
}
