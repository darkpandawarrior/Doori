package com.mileway

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.mileway.core.data.database.buildMilewayDatabase
import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.session.CurrentTrackDataStore
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.service.SessionReconciliationPolicy
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * G9: the app's first instrumented integration test — the offline tracking lifecycle on a real device DB.
 *
 * Runs on a device or the Gradle Managed Device (`pixel6Api34noGmsDebugAndroidTest`), NOT the JVM unit-test
 * gate. It exercises the headline offline promise from CLAUDE.md ("track a trip, kill and relaunch, confirm
 * the record persisted"):
 *
 *  1. A **FusedLocation test double** (`setMockMode` + `setMockLocation`) produces a sequence of GPS fixes.
 *  2. Those fixes are persisted through the real Room DB + [LocationRepository], exactly as the foreground
 *     tracking service does, and the trip is finalized as a [SavedTrack].
 *  3. The DB is closed and reopened — simulating the app being killed and relaunched.
 *  4. The trip and its full location trail must survive, with a coherent computed distance.
 *
 * Mock-location injection needs the app to be the device's mock-location app, which the GMD/CI sets up; the
 * `setMock*` calls are best-effort (the persistence assertions are what gate the test) so the test stays
 * robust across environments.
 */
@RunWith(AndroidJUnit4::class)
class TrackingLifecycleTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fused = LocationServices.getFusedLocationProviderClient(context)
    private val token = "instrumented-lifecycle-trip"

    @Before
    fun clean() {
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        runCatching { Tasks.await(fused.setMockMode(false)) }
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun mockGpsTrip_persistsAcrossKillAndRelaunch() {
        // 1. FusedLocation test double → a short straight-line route of mock fixes.
        runCatching { Tasks.await(fused.setMockMode(true)) }
        val baseTime = 1_700_000_000_000L
        val fixes =
            (0 until POINT_COUNT).map { i ->
                Location("mock").apply {
                    latitude = 18.5204 + i * 0.0005
                    longitude = 73.8567 + i * 0.0005
                    accuracy = 5f
                    speed = 8f
                    time = baseTime + i * 1_000L
                }
            }
        fixes.forEach { runCatching { Tasks.await(fused.setMockLocation(it)) } }

        val distanceMeters =
            fixes.zipWithNext { a, b -> a.distanceTo(b).toDouble() }.sum()

        // 2. Persist the fixes + finalize the trip via the real Room DB, as the service would.
        var db = buildMilewayDatabase(context)
        runBlocking {
            val locations =
                fixes.mapIndexed { i, f ->
                    LocationData(
                        activity = "IN_VEHICLE",
                        speed = f.speed,
                        lat = f.latitude,
                        lng = f.longitude,
                        token = token,
                        batteryPercentage = 100.0,
                        date = f.time,
                    )
                }
            LocationRepository(db.locationDao()).insertBatch(locations)
            db.savedTrackDao().insertSavedTrack(
                SavedTrack(
                    routeId = token,
                    name = "Instrumented mock trip",
                    startLatitude = fixes.first().latitude,
                    startLongitude = fixes.first().longitude,
                    endLatitude = fixes.last().latitude,
                    endLongitude = fixes.last().longitude,
                    pausedLatitude = 0.0,
                    pausedLongitude = 0.0,
                    startTime = fixes.first().time,
                    endTime = fixes.last().time,
                    distance = distanceMeters,
                    duration = fixes.last().time - fixes.first().time,
                    isCompleted = true,
                ),
            )
        }

        // 3. Simulate process death + relaunch: close and reopen the on-disk DB.
        db.close()
        db = buildMilewayDatabase(context)

        // 4. The trip and its full trail must have survived.
        runBlocking {
            val track = db.savedTrackDao().getSavedTrackById(token)
            assertNotNull("Completed trip should persist across a DB reopen", track)
            assertTrue("Trip should be marked completed", track!!.isCompleted)
            assertTrue("Persisted distance should be positive", track.distance > 0.0)

            val trail = LocationRepository(db.locationDao()).getForToken(token)
            assertEquals("Every GPS fix should persist", POINT_COUNT, trail.size)
            assertEquals("Trail should be chronological", fixes.first().time, trail.first().date)
        }
        db.close()
    }

    /**
     * P-G.1 / L1: `onTaskRemoved` (app swiped from recents) writes the `wasAppKilled` flag to the
     * active Room row. The flag must survive process death so app-launch reconciliation can detect the
     * interruption. Exercises the real DAO write-path (`markAppKilled`) against the on-disk DB.
     */
    @Test
    fun onTaskRemoved_setsAppKilledFlag_persistsAcrossRelaunch() {
        var db = buildMilewayDatabase(context)
        runBlocking {
            db.savedTrackDao().insertSavedTrack(
                SavedTrack(
                    routeId = token,
                    name = "Interrupted trip",
                    startLatitude = 18.5204,
                    startLongitude = 73.8567,
                    endLatitude = 0.0,
                    endLongitude = 0.0,
                    pausedLatitude = 0.0,
                    pausedLongitude = 0.0,
                    startTime = 1_700_000_000_000L,
                    endTime = 0L,
                    distance = 0.0,
                    duration = 0L,
                    isCompleted = false,
                ),
            )
            // As LocationTrackingService.onTaskRemoved does for the active token.
            assertEquals("markAppKilled should update exactly the active row", 1, db.savedTrackDao().markAppKilled(token))
        }

        // Simulate process death + relaunch.
        db.close()
        db = buildMilewayDatabase(context)

        runBlocking {
            val track = db.savedTrackDao().getSavedTrackById(token)
            assertNotNull("Interrupted trip should persist across a DB reopen", track)
            assertTrue("wasAppKilled flag must survive the relaunch", track!!.wasAppKilled)
            assertTrue("Trip should still be ongoing (not completed)", !track.isCompleted)
        }
        db.close()
    }

    /**
     * P-G.1 / L5: ghost-relaunch. After an app-kill, the DataStore still claims `isTracking`, but the
     * Room row carries the interruption flag. [SessionReconciliationPolicy] (the shared launch hook for
     * both platforms) must classify this as `NeedsDecision` so the user gets the session-restore sheet —
     * not a silent resume, and not a silent discard.
     */
    @Test
    fun ghostSession_afterAppKill_reconcilesToNeedsDecision() {
        val store = CurrentTrackDataStore(context)
        val db = buildMilewayDatabase(context)
        runBlocking {
            store.clearSession()
            store.saveSession(
                CurrentTrackData(
                    token = token,
                    startTime = 1_700_000_000_000L,
                    isTracking = true,
                ),
            )
            db.savedTrackDao().insertSavedTrack(
                SavedTrack(
                    routeId = token,
                    name = "Ghost trip",
                    startLatitude = 18.5204,
                    startLongitude = 73.8567,
                    endLatitude = 0.0,
                    endLongitude = 0.0,
                    pausedLatitude = 0.0,
                    pausedLongitude = 0.0,
                    startTime = 1_700_000_000_000L,
                    endTime = 0L,
                    distance = 0.0,
                    duration = 0L,
                    isCompleted = false,
                ),
            )
            db.savedTrackDao().markAppKilled(token)

            val policy = SessionReconciliationPolicy(store, SavedTrackRepository(db.savedTrackDao()))
            val outcome = policy.reconcile()

            assertTrue(
                "An app-killed ghost session must require a user decision, got $outcome",
                outcome is SessionReconciliationPolicy.Outcome.NeedsDecision,
            )
            assertEquals(token, (outcome as SessionReconciliationPolicy.Outcome.NeedsDecision).token)
            assertTrue("Reason should name the app-kill", outcome.reason.contains("app-kill"))

            store.clearSession()
        }
        db.close()
    }

    private companion object {
        const val DB_NAME = "mileway.db"
        const val POINT_COUNT = 12
    }
}
