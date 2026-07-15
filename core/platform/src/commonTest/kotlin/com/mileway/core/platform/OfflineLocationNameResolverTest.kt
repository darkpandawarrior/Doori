package com.mileway.core.platform

import com.siddharth.kmp.appshell.PlaceName
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OfflineLocationNameResolverTest {
    private val resolver = OfflineLocationNameResolver()

    @Test
    fun resolves_pune_city_centre_to_a_named_place() =
        runTest {
            val place = resolver.resolve(18.5204, 73.8567)
            assertEquals("Pune City Centre", place.name)
            assertEquals("18.5204, 73.8567", place.coordinates)
            assertEquals("Pune City Centre", place.displayLabel)
        }

    @Test
    fun resolves_koregaon_park_coordinates_to_koregaon_park() =
        runTest {
            // A point on the simulated NE drive, near Koregaon Park.
            val place = resolver.resolve(18.5365, 73.8940)
            assertEquals("Koregaon Park, Pune", place.name)
        }

    @Test
    fun remote_source_returns_a_richer_label_than_the_local_table() =
        runTest {
            // PLAN_V24 P10.5: the reverse-geocode source toggle. Local table = the gazetteer label;
            // simulated-remote = the same match with the administrative region appended.
            val remote = OfflineLocationNameResolver(remoteSourceEnabled = { true })
            assertEquals("Pune City Centre", resolver.resolve(18.5204, 73.8567).name)
            assertEquals("Pune City Centre, Maharashtra", remote.resolve(18.5204, 73.8567).name)
        }

    @Test
    fun identical_coordinates_resolve_deterministically() =
        runTest {
            val a = resolver.resolve(18.5479, 73.9010)
            val b = resolver.resolve(18.5479, 73.9010)
            assertEquals(a.name, b.name)
            assertEquals("Kalyani Nagar, Pune", a.name)
        }

    @Test
    fun far_away_coordinates_fall_back_to_coordinates_only() =
        runTest {
            // Mumbai — well outside the Pune gazetteer radius.
            val place = resolver.resolve(19.0760, 72.8777)
            assertNull(place.name)
            assertEquals("19.0760, 72.8777", place.coordinates)
            assertEquals("19.0760, 72.8777", place.displayLabel)
        }

    @Test
    fun every_gazetteer_waypoint_resolves_to_its_own_name() =
        runTest {
            OfflineLocationNameResolver.PUNE_WAYPOINTS.forEach { wp ->
                val place = resolver.resolve(wp.latitude, wp.longitude)
                assertEquals(wp.name, place.name, "exact waypoint ${wp.name} should resolve to itself")
            }
        }

    @Test
    fun resolver_never_throws_for_extreme_inputs() =
        runTest {
            assertNotNull(resolver.resolve(0.0, 0.0))
            assertNotNull(resolver.resolve(-89.9, 179.9))
            assertNotNull(resolver.resolve(90.0, -180.0))
        }

    @Test
    fun placeName_formatCoordinates_pads_to_four_decimals() {
        assertEquals("18.5200, 73.8000", PlaceName.formatCoordinates(18.52, 73.80))
        assertEquals("0.0000, 0.0000", PlaceName.formatCoordinates(0.0, 0.0))
    }

    @Test
    fun placeName_displayLabel_prefers_name_else_coordinates() {
        assertEquals("Koregaon Park", PlaceName("Koregaon Park", "18.5, 73.9").displayLabel)
        assertTrue(PlaceName.coordinatesOnly(18.5, 73.9).displayLabel.contains(","))
    }
}
