package com.miletracker

import com.miletracker.core.data.model.db.EventAudience
import com.miletracker.core.data.model.db.EventType
import com.miletracker.core.data.model.db.HardwareEvent
import com.miletracker.core.data.model.db.LocationData
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.feature.tracking.export.CsvExporter
import com.miletracker.feature.tracking.export.GeoJsonExporter
import com.miletracker.feature.tracking.export.GpxExporter
import com.miletracker.feature.tracking.export.JsonExporter
import com.miletracker.feature.tracking.export.KmlExporter
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the 5 text-based export formats (CSV, GPX, KML, GeoJSON, JSON).
 * Pure JVM, no Android framework needed.
 */
class ExportFormatsTest {

    // -------------------------------------------------------------------------
    // Shared test data builders
    // -------------------------------------------------------------------------

    private fun makeTrack(): SavedTrack = SavedTrack(
        routeId = "route-abc-123",
        name = "Morning Commute",
        startLatitude = 18.5200, startLongitude = 73.8560,
        endLatitude = 18.5400, endLongitude = 73.8900,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = 1_700_000_000_000L,
        endTime   = 1_700_003_600_000L,
        distance  = 5_250.0,
        duration  = 3_600_000L
    )

    private fun makeLocations(count: Int = 5): List<LocationData> =
        (0 until count).map { i ->
            LocationData(
                id = i.toLong(),
                activity = "Driving",
                speed = 11.5f,
                lat = 18.52 + i * 0.001,
                lng = 73.856 + i * 0.001,
                token = "route-abc-123",
                date = 1_700_000_000_000L + i * 60_000L,
                accuracy = 5f,
                altitude = 570.0 + i,
                batteryPercentage = 80.0 - i,
                isMock = false,
                isAbnormal = false
            )
        }

    private fun makeEvents(): List<HardwareEvent> = listOf(
        HardwareEvent(
            id = 1L,
            token = "route-abc-123",
            event = "Tracking Started",
            eventType = EventType.TRACKING_STARTED,
            time = 1_700_000_000_000L,
            audience = EventAudience.USER
        ),
        HardwareEvent(
            id = 2L,
            token = "route-abc-123",
            event = "Tracking Stopped",
            eventType = EventType.TRACKING_STOPPED,
            time = 1_700_003_600_000L,
            audience = EventAudience.USER
        )
    )

    // -------------------------------------------------------------------------
    // CSV tests
    // -------------------------------------------------------------------------

    @Test
    fun `csv output has correct header`() {
        val csv = CsvExporter.export(makeTrack(), makeLocations(), makeEvents())
        val lines = csv.lines().filter { it.isNotBlank() }
        val header = lines.first { !it.startsWith("#") }
        assertTrue(header.startsWith("id,token,timestamp_ms"), "Expected CSV header, got: $header")
        assertTrue(header.contains("latitude"), "Header should contain 'latitude'")
        assertTrue(header.contains("longitude"), "Header should contain 'longitude'")
        assertTrue(header.contains("accuracy_m"), "Header should contain 'accuracy_m'")
    }

    @Test
    fun `csv output has one row per location`() {
        val locations = makeLocations(7)
        val csv = CsvExporter.export(makeTrack(), locations, makeEvents())
        val dataLines = csv.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .drop(1) // skip header
        assertEquals(7, dataLines.size, "Expected 7 data rows, got ${dataLines.size}")
    }

    @Test
    fun `csv row contains correct lat and lng values`() {
        val locations = makeLocations(1)
        val csv = CsvExporter.export(makeTrack(), locations, makeEvents())
        val dataLine = csv.lines().first { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("id,") }
        assertTrue(dataLine.contains("18.52"), "Row should contain start latitude 18.52")
        assertTrue(dataLine.contains("73.856"), "Row should contain start longitude 73.856")
    }

    @Test
    fun `csv with empty locations produces only header`() {
        val csv = CsvExporter.export(makeTrack(), emptyList(), emptyList())
        val nonCommentLines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        assertEquals(1, nonCommentLines.size, "Should have just the header for empty locations")
    }

    // -------------------------------------------------------------------------
    // GPX tests
    // -------------------------------------------------------------------------

    @Test
    fun `gpx output contains valid root element`() {
        val gpx = GpxExporter.export(makeTrack(), makeLocations(), makeEvents())
        assertTrue(gpx.contains("<gpx "), "GPX should start with <gpx element")
        assertTrue(gpx.contains("</gpx>"), "GPX should end with </gpx>")
        assertTrue(gpx.contains("<trk>"), "GPX should have <trk>")
        assertTrue(gpx.contains("<trkseg>"), "GPX should have <trkseg>")
    }

    @Test
    fun `gpx output has correct number of trkpt elements`() {
        val locations = makeLocations(5)
        val gpx = GpxExporter.export(makeTrack(), locations, makeEvents())
        val trkptCount = gpx.split("<trkpt").size - 1
        assertEquals(5, trkptCount, "Expected 5 <trkpt> elements, got $trkptCount")
    }

    @Test
    fun `gpx trkpt has lat and lon attributes`() {
        val locations = makeLocations(1)
        val gpx = GpxExporter.export(makeTrack(), locations, makeEvents())
        assertTrue(gpx.contains("lat=\"18.52\""), "Should contain lat attribute")
        assertTrue(gpx.contains("lon=\"73.856\""), "Should contain lon attribute")
    }

    @Test
    fun `gpx contains time element for each point`() {
        val gpx = GpxExporter.export(makeTrack(), makeLocations(3), makeEvents())
        val timeCount = gpx.split("<time>").size - 1
        assertEquals(3, timeCount, "Expected 3 <time> elements, got $timeCount")
    }

    @Test
    fun `gpx contains ele element for points with altitude`() {
        val gpx = GpxExporter.export(makeTrack(), makeLocations(2), makeEvents())
        assertTrue(gpx.contains("<ele>"), "GPX should contain <ele> for non-zero altitudes")
    }

    @Test
    fun `gpx track name is set`() {
        val gpx = GpxExporter.export(makeTrack(), makeLocations(), makeEvents())
        assertTrue(gpx.contains("<name>Morning Commute</name>"), "GPX should contain track name")
    }

    // -------------------------------------------------------------------------
    // KML tests
    // -------------------------------------------------------------------------

    @Test
    fun `kml output has valid root structure`() {
        val kml = KmlExporter.export(makeTrack(), makeLocations(), makeEvents())
        assertTrue(kml.contains("<kml "), "KML should start with <kml")
        assertTrue(kml.contains("</kml>"), "KML should end with </kml>")
        assertTrue(kml.contains("<Document>"), "KML should have <Document>")
        assertTrue(kml.contains("<Placemark>"), "KML should have <Placemark>")
    }

    @Test
    fun `kml contains LineString with coordinates`() {
        val kml = KmlExporter.export(makeTrack(), makeLocations(3), makeEvents())
        assertTrue(kml.contains("<LineString>"), "KML should contain <LineString>")
        assertTrue(kml.contains("<coordinates>"), "KML should contain <coordinates>")
    }

    @Test
    fun `kml coordinates are in lng,lat,alt order`() {
        val locations = makeLocations(1)
        val kml = KmlExporter.export(makeTrack(), locations, makeEvents())
        // first point: lat=18.52, lng=73.856, alt=570.0
        assertTrue(
            kml.contains("73.856,18.52,570.0"),
            "KML coordinates should be lng,lat,alt order. KML was:\n$kml"
        )
    }

    @Test
    fun `kml has start and end placemarks`() {
        val kml = KmlExporter.export(makeTrack(), makeLocations(3), makeEvents())
        assertTrue(kml.contains("<name>Start</name>"), "KML should have Start placemark")
        assertTrue(kml.contains("<name>End</name>"), "KML should have End placemark")
    }

    @Test
    fun `kml with empty locations has no coordinates`() {
        val kml = KmlExporter.export(makeTrack(), emptyList(), makeEvents())
        assertFalse(kml.contains("<name>Start</name>"), "No Start placemark for empty track")
        assertFalse(kml.contains("<name>End</name>"), "No End placemark for empty track")
    }

    // -------------------------------------------------------------------------
    // GeoJSON tests
    // -------------------------------------------------------------------------

    @Test
    fun `geojson output is valid FeatureCollection`() {
        val geo = GeoJsonExporter.export(makeTrack(), makeLocations(), makeEvents())
        assertTrue(geo.contains("\"type\": \"FeatureCollection\""), "Should be FeatureCollection")
        assertTrue(geo.contains("\"features\": ["), "Should have features array")
    }

    @Test
    fun `geojson contains LineString feature`() {
        val geo = GeoJsonExporter.export(makeTrack(), makeLocations(5), makeEvents())
        assertTrue(geo.contains("\"type\": \"LineString\""), "Should contain LineString geometry")
    }

    @Test
    fun `geojson contains start and end point features`() {
        val geo = GeoJsonExporter.export(makeTrack(), makeLocations(5), makeEvents())
        assertTrue(geo.contains("\"label\": \"Start\""), "Should have Start point feature")
        assertTrue(geo.contains("\"label\": \"End\""), "Should have End point feature")
    }

    @Test
    fun `geojson coordinates are in lng,lat order`() {
        val locations = makeLocations(1)
        val geo = GeoJsonExporter.export(makeTrack(), locations, makeEvents())
        // first point: lat=18.52, lng=73.856
        assertTrue(
            geo.contains("[73.856,18.52") || geo.contains("[73.856, 18.52"),
            "GeoJSON coordinates should be [lng, lat]. Output:\n$geo"
        )
    }

    @Test
    fun `geojson track properties include route metadata`() {
        val geo = GeoJsonExporter.export(makeTrack(), makeLocations(), makeEvents())
        assertTrue(geo.contains("route-abc-123"), "Should contain routeId")
        assertTrue(geo.contains("Morning Commute"), "Should contain track name")
    }

    // -------------------------------------------------------------------------
    // JSON tests
    // -------------------------------------------------------------------------

    @Test
    fun `json output has track metadata section`() {
        val json = JsonExporter.export(makeTrack(), makeLocations(), makeEvents())
        assertTrue(json.contains("\"track\": {"), "JSON should have track object")
        assertTrue(json.contains("\"routeId\":"), "JSON track should have routeId")
        assertTrue(json.contains("route-abc-123"), "Should contain the actual routeId value")
    }

    @Test
    fun `json output has points array`() {
        val json = JsonExporter.export(makeTrack(), makeLocations(4), makeEvents())
        assertTrue(json.contains("\"points\": ["), "JSON should have points array")
    }

    @Test
    fun `json output has events array`() {
        val json = JsonExporter.export(makeTrack(), makeLocations(), makeEvents())
        assertTrue(json.contains("\"events\": ["), "JSON should have events array")
    }

    @Test
    fun `json points array contains all location entries`() {
        val locations = makeLocations(6)
        val json = JsonExporter.export(makeTrack(), locations, makeEvents())
        // Each entry starts with "id":, count them in points section
        val pointsSection = json.substringAfter("\"points\": [").substringBefore("\"events\":")
        val idCount = pointsSection.split("\"id\":").size - 1
        assertEquals(6, idCount, "Expected 6 'id' entries in points, found $idCount")
    }

    @Test
    fun `json events array contains all event entries`() {
        val events = makeEvents()
        val json = JsonExporter.export(makeTrack(), makeLocations(), events)
        val eventsSection = json.substringAfter("\"events\": [")
        val eventCount = eventsSection.split("\"eventType\":").size - 1
        assertEquals(2, eventCount, "Expected 2 events, found $eventCount")
    }

    @Test
    fun `json output contains lat and lng for each point`() {
        val json = JsonExporter.export(makeTrack(), makeLocations(1), makeEvents())
        assertTrue(json.contains("\"lat\": 18.52"), "JSON point should contain lat")
        assertTrue(json.contains("\"lng\": 73.856"), "JSON point should contain lng")
    }
}
