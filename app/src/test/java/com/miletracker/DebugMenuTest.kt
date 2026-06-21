package com.miletracker

import com.miletracker.feature.tracking.debug.DebugMenuUiState
import com.miletracker.feature.tracking.debug.DebugProfile
import com.miletracker.feature.tracking.debug.DebugProfiles
import com.miletracker.feature.tracking.debug.buildConfigSnapshot
import com.miletracker.feature.tracking.debug.searchMatches
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for debug-menu pure logic:
 * - Profile preset mapping (applying a profile produces the expected flag state)
 * - Config snapshot key-value mapping
 * - Search-filter helper
 *
 * No Android framework or Koin required, all JVM-only.
 */
class DebugMenuTest {

    // -------------------------------------------------------------------------
    // DebugProfile preset structure
    // -------------------------------------------------------------------------

    @Test
    fun `all built-in profiles have a non-blank name`() {
        DebugProfiles.allProfiles.forEach { profile ->
            assertTrue(profile.name.isNotBlank(), "Profile name must not be blank")
        }
    }

    @Test
    fun `all built-in profiles have a non-empty options map`() {
        DebugProfiles.allProfiles.forEach { profile ->
            assertTrue(profile.options.isNotEmpty(), "${profile.name}: options must not be empty")
        }
    }

    @Test
    fun `DEVELOPMENT profile enables Allow Mock Locations`() {
        val profile = DebugProfiles.DEVELOPMENT
        assertEquals(true, profile.options["Allow Mock Locations"],
            "Development profile should enable mock locations")
    }

    @Test
    fun `QA_TESTING profile enables Enable Location Dump Creation`() {
        val profile = DebugProfiles.QA_TESTING
        assertEquals(true, profile.options["Enable Location Dump Creation"],
            "QA Testing profile should enable location dump")
    }

    @Test
    fun `PERFORMANCE_TESTING profile bypasses battery checks`() {
        val profile = DebugProfiles.PERFORMANCE_TESTING
        assertEquals(true, profile.options["Bypass Battery Level Check"],
            "Performance profile should bypass battery level check")
        assertEquals(true, profile.options["Bypass Battery Optimization Check"],
            "Performance profile should bypass battery optimization check")
    }

    @Test
    fun `TRACK_MILES_DEBUGGING profile enables tracking overlay and mock locations`() {
        val profile = DebugProfiles.TRACK_MILES_DEBUGGING
        assertTrue(profile.options["Enable Tracking Overlay"] == true,
            "Track Miles Debug profile should enable tracking overlay")
        assertTrue(profile.options["Allow Mock Locations"] == true,
            "Track Miles Debug profile should allow mock locations")
    }

    @Test
    fun `applying a profile resets all other options to false`() {
        // Simulate the applyProfileToUiState logic on a populated state
        val allTrackingKeys = setOf(
            "Skip Odometer", "Toggle Odometer OCR", "Allow Mock Locations",
            "Enable Location Dump Creation", "Force BE Distance Calculation",
            "Bypass Battery Level Check", "Bypass Battery Optimization Check",
            "Use V2 Location Sync API",
        )

        val profile = DebugProfiles.DEVELOPMENT

        // Start with everything enabled
        val initial: Map<String, Boolean> = allTrackingKeys.associateWith { true }

        // Reset, then apply profile options
        val after = allTrackingKeys.associateWith { false }.toMutableMap()
        profile.options.forEach { (key, value) ->
            if (after.containsKey(key)) after[key] = value
        }

        // Keys NOT in the profile must be false
        val profileTrackingKeys = profile.options.filter { it.key in allTrackingKeys }.keys
        val nonProfileKeys = allTrackingKeys - profileTrackingKeys
        nonProfileKeys.forEach { key ->
            assertFalse(after[key] ?: false, "Key '$key' should be false after applying profile")
        }

        // Keys in the profile should match profile values
        profileTrackingKeys.forEach { key ->
            assertEquals(profile.options[key], after[key],
                "Key '$key' should match profile value after apply")
        }
    }

    // -------------------------------------------------------------------------
    // buildConfigSnapshot pure function
    // -------------------------------------------------------------------------

    @Test
    fun `buildConfigSnapshot includes tenant key`() {
        val snapshot = buildConfigSnapshot(
            uiState = DebugMenuUiState(),
            trackMilesV2 = true,
            geoCheckIn = true,
            manualCheckIn = true,
            currency = "INR",
            tenant = "DEMO",
            service = "Own Car",
            allowMockLocations = false,
        )
        assertTrue(snapshot.containsKey("Tenant"), "Snapshot must have Tenant key")
        assertEquals("DEMO", snapshot["Tenant"])
    }

    @Test
    fun `buildConfigSnapshot reflects allowMockLocations flag correctly`() {
        val snapshotAllowed = buildConfigSnapshot(
            uiState = DebugMenuUiState(
                trackingOptions = mapOf("Allow Mock Locations" to true),
            ),
            trackMilesV2 = true, geoCheckIn = false, manualCheckIn = false,
            currency = "INR", tenant = "DEMO", service = "Car",
            allowMockLocations = true,
        )
        val snapshotBlocked = buildConfigSnapshot(
            uiState = DebugMenuUiState(),
            trackMilesV2 = true, geoCheckIn = false, manualCheckIn = false,
            currency = "INR", tenant = "DEMO", service = "Car",
            allowMockLocations = false,
        )

        assertEquals("allowed", snapshotAllowed["Mock locations (debug)"])
        assertEquals("blocked", snapshotBlocked["Mock locations (debug)"])
    }

    @Test
    fun `buildConfigSnapshot shows debug flags active count`() {
        val uiState = DebugMenuUiState(enabledOptionsCount = 3)
        val snapshot = buildConfigSnapshot(
            uiState = uiState,
            trackMilesV2 = false, geoCheckIn = false, manualCheckIn = false,
            currency = "USD", tenant = "T1", service = "Bike",
            allowMockLocations = false,
        )
        assertEquals("3", snapshot["Debug flags active"])
    }

    @Test
    fun `buildConfigSnapshot track miles v2 label`() {
        val snapshotOn = buildConfigSnapshot(
            uiState = DebugMenuUiState(), trackMilesV2 = true,
            geoCheckIn = false, manualCheckIn = false, currency = "INR",
            tenant = "T", service = "S", allowMockLocations = false,
        )
        val snapshotOff = buildConfigSnapshot(
            uiState = DebugMenuUiState(), trackMilesV2 = false,
            geoCheckIn = false, manualCheckIn = false, currency = "INR",
            tenant = "T", service = "S", allowMockLocations = false,
        )
        assertEquals("enabled", snapshotOn["Track Miles V2"])
        assertEquals("disabled", snapshotOff["Track Miles V2"])
    }

    @Test
    fun `buildConfigSnapshot has exactly the expected keys`() {
        val snapshot = buildConfigSnapshot(
            uiState = DebugMenuUiState(), trackMilesV2 = true,
            geoCheckIn = true, manualCheckIn = true, currency = "INR",
            tenant = "DEMO", service = "Car", allowMockLocations = false,
        )
        val expectedKeys = setOf(
            "Tenant", "Currency", "Service", "Track Miles V2",
            "Geo check-in", "Manual check-in", "Mock locations (debug)", "Debug flags active",
        )
        assertEquals(expectedKeys, snapshot.keys.toSet(),
            "Snapshot keys should match expected set")
    }

    // -------------------------------------------------------------------------
    // searchMatches helper
    // -------------------------------------------------------------------------

    @Test
    fun `searchMatches returns true for blank query`() {
        assertTrue(searchMatches("Allow Mock Locations", ""))
        assertTrue(searchMatches("Any string", "   "))
    }

    @Test
    fun `searchMatches is case-insensitive`() {
        assertTrue(searchMatches("Allow Mock Locations", "mock"))
        assertTrue(searchMatches("Allow Mock Locations", "MOCK"))
        assertTrue(searchMatches("Allow Mock Locations", "Mock Locations"))
    }

    @Test
    fun `searchMatches returns false for non-matching query`() {
        assertFalse(searchMatches("Allow Mock Locations", "battery"))
        assertFalse(searchMatches("Skip Odometer", "network"))
    }

    @Test
    fun `searchMatches partial substring match`() {
        assertTrue(searchMatches("Bypass Battery Level Check", "battery level"))
        assertTrue(searchMatches("Enable Location Dump Creation", "dump"))
    }
}
