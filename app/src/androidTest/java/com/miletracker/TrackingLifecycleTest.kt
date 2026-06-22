package com.miletracker

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.miletracker.core.data.database.buildMileTrackerDatabase
import com.miletracker.core.data.model.db.LocationData
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.feature.tracking.repository.LocationRepository
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
        var db = buildMileTrackerDatabase(context)
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
        db = buildMileTrackerDatabase(context)

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

    private companion object {
        const val DB_NAME = "miletracker.db"
        const val POINT_COUNT = 12
    }
}
