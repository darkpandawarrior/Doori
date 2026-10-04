package com.mileway

import com.mileway.core.data.dao.HardwareEventDao
import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedPlaceEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.platform.ShareSheet
import com.mileway.feature.tracking.repository.HardwareEventRepository
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.ui.components.ExportFormat
import com.mileway.feature.tracking.ui.components.LocationDataFilter
import com.mileway.feature.tracking.viewmodel.ExportViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExportHomeWiringTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `export uses saved Home by default and honors opt out and no Home`() = runTest {
        val home = SavedPlaceEntity("home", "HOME", "Home", "12 Garden Road", 18.5204, 73.8567, 0L)
        val track = SavedTrack(
            routeId = "trip", name = home.address,
            startLatitude = 18.5205, startLongitude = 73.8567, endLatitude = 18.53, endLongitude = 73.86,
            pausedLatitude = 0.0, pausedLongitude = 0.0, startTime = 0L, endTime = 1000L,
            distance = 1000.0, duration = 1000L,
        )
        val points = listOf(
            LocationData(activity = "walking", speed = 0f, lat = 18.5205, lng = 73.8567, token = "trip", batteryPercentage = 90.0),
            LocationData(activity = "walking", speed = 0f, lat = 18.53, lng = 73.86, token = "trip", batteryPercentage = 90.0),
        )
        val tracks = mockk<SavedTrackDao>()
        val locations = mockk<LocationDao>()
        val events = mockk<HardwareEventDao>()
        coEvery { tracks.getSavedTrackById("trip") } returns track
        coEvery { locations.getLocationsByTokenPaged("trip", Int.MAX_VALUE, 0) } returns points
        coEvery { events.getEventsByToken("trip") } returns emptyList()
        val savedPlaces = FakeSavedPlaceDao()
        savedPlaces.upsert(home)
        var shared = ""
        var sharedSubject: String? = null
        val share = object : ShareSheet {
            override fun share(text: String, subject: String?, fileUri: String?) {
                shared = text
                sharedSubject = subject
            }
        }
        val vm = ExportViewModel(SavedTrackRepository(tracks), LocationRepository(locations), HardwareEventRepository(events), share, savedPlaces)
        vm.export("trip", ExportFormat.JSON, LocationDataFilter())
        runCurrent()
        assertNull(vm.state.value.error)
        assertFalse(shared.contains("18.5205"))
        assertFalse(shared.contains(home.address))
        assertTrue(sharedSubject?.contains("Home") == true)
        vm.export("trip", ExportFormat.JSON, LocationDataFilter(redactHome = false))
        runCurrent()
        assertTrue(shared.contains("18.5205"))
        assertTrue(shared.contains(home.address))
        savedPlaces.delete("home")
        vm.export("trip", ExportFormat.JSON, LocationDataFilter())
        runCurrent()
        assertTrue(shared.contains("18.5205"))
        assertTrue(shared.contains(home.address))
    }
}
