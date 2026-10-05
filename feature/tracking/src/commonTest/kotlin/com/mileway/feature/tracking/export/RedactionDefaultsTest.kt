package com.mileway.feature.tracking.export

import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedPlaceEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.feature.tracking.ui.components.ExportFormat
import com.mileway.feature.tracking.ui.components.LocationDataFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RedactionDefaultsTest {
    private val home = SavedPlaceEntity("home", "HOME", "Home", "12 Garden Road", 18.5204, 73.8567, 0L)
    private val near = point(18.5205, 73.8567)
    private val far = point(18.53, 73.86)

    private fun point(
        lat: Double,
        lng: Double,
    ) = LocationData(
        activity = "walking",
        speed = 0f,
        lat = lat,
        lng = lng,
        token = "r1",
        batteryPercentage = 90.0,
    )

    @Test
    fun `shared export boundary protects all formats and embedded Home addresses`() {
        val source =
            SavedTrack(
                routeId = "protected",
                name = "Arrival at ${home.address}",
                startLatitude = near.lat,
                startLongitude = near.lng,
                endLatitude = far.lat,
                endLongitude = far.lng,
                pausedLatitude = 0.0,
                pausedLongitude = 0.0,
                startTime = 1L,
                endTime = 2L,
                distance = 1000.0,
                duration = 1L,
            )
        val safe = RedactionDefaults.protect(source, listOf(near, far), emptyList(), listOf(home))
        for (format in ExportFormat.entries) {
            val content = TrackExportContent.build(format, safe.track, safe.points, safe.events, safe.omitEndpointCoordinates)
            assertFalse(content.contains(home.address))
            assertFalse(content.contains(near.lat.toString()))
        }
        assertEquals("Arrival at ${home.address}", source.name)
    }

    @Test
    fun `Home points are dropped by default and the option defaults on`() {
        assertTrue(LocationDataFilter().redactHome)
        assertEquals(listOf(far), RedactionDefaults.locations(listOf(near, far), home))
    }

    @Test
    fun `flag off keeps Home points and address`() {
        assertEquals(listOf(near, far), RedactionDefaults.locations(listOf(near, far), home, false))
        assertEquals(home.address, RedactionDefaults.address(home.address, home, false))
    }

    @Test
    fun `no Home keeps all data and a work place is never treated as Home`() {
        val points = listOf(near, far)
        assertEquals(points, RedactionDefaults.locations(points, null))
        assertEquals(home.address, RedactionDefaults.address(home.address, null))
        assertEquals(points, RedactionDefaults.locations(points, home.copy(type = "WORK")))
    }

    @Test
    fun `address only Home still replaces address but retains points`() {
        val addressOnly = home.copy(latitude = null, longitude = null)
        assertEquals(listOf(near, far), RedactionDefaults.locations(listOf(near, far), addressOnly))
        assertEquals("Home", RedactionDefaults.address(home.address, addressOnly))
        assertEquals("Other address", RedactionDefaults.address("Other address", home))
    }

    @Test
    fun `every format excludes the Home point and address including JSON endpoints and events`() {
        val track =
            SavedTrack(
                routeId = "r1",
                name = home.address,
                startLatitude = near.lat,
                startLongitude = near.lng,
                endLatitude = far.lat,
                endLongitude = far.lng,
                pausedLatitude = 0.0,
                pausedLongitude = 0.0,
                startTime = 0L,
                endTime = 1000L,
                distance = 1000.0,
                duration = 1000L,
            )
        val points = RedactionDefaults.locations(listOf(near, far), home)
        val cleanTrack = RedactionDefaults.track(track, points, home)
        val cleanEvents = RedactionDefaults.events(listOf(HardwareEvent(token = "r1", event = home.address, lat = near.lat, lng = near.lng)), home)
        assertNull(cleanEvents.single().lat)
        assertNull(cleanEvents.single().lng)
        assertEquals("Home", cleanEvents.single().event)
        for (format in ExportFormat.entries) {
            val content = TrackExportContent.build(format, cleanTrack, points, cleanEvents)
            assertFalse(content.contains(near.lat.toString()), "$format leaked Home latitude")
            assertFalse(content.contains(home.address), "$format leaked Home address")
            assertTrue(content.contains("Home"))
        }
    }

    @Test
    fun `all Home points can be redacted without retaining Home endpoint coordinates`() {
        assertEquals(emptyList(), RedactionDefaults.locations(listOf(near), home))
    }

    @Test
    fun `empty redacted route omits JSON endpoint coordinates instead of inventing a location`() {
        val track =
            SavedTrack(
                routeId = "r1",
                name = "Home",
                startLatitude = 0.0,
                startLongitude = 0.0,
                endLatitude = 0.0,
                endLongitude = 0.0,
                pausedLatitude = 0.0,
                pausedLongitude = 0.0,
                startTime = 0L,
                endTime = 1000L,
                distance = 0.0,
                duration = 1000L,
            )
        val content = TrackExportContent.build(ExportFormat.JSON, track, emptyList(), emptyList(), omitEndpointCoordinates = true)
        assertTrue(content.contains("\"startLat\": null"))
        assertTrue(content.contains("\"startLng\": null"))
        assertTrue(content.contains("\"endLat\": null"))
        assertTrue(content.contains("\"endLng\": null"))
        val unchanged = TrackExportContent.build(ExportFormat.JSON, track, emptyList(), emptyList())
        assertTrue(unchanged.contains("\"startLat\": 0.0"))
    }
}
