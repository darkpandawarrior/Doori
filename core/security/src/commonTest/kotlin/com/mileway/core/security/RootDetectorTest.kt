package com.mileway.core.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RootDetectorTest {
    @Test
    fun check_returns_a_non_null_result() {
        val result = RootDetector.check()
        assertNotNull(result)
    }

    @Test
    fun isRooted_reflects_whether_signals_list_is_non_empty() {
        val result = RootDetector.check()
        // Structural invariant: isRooted must be true iff signals is non-empty.
        if (result.signals.isEmpty()) {
            assertFalse(result.isRooted, "isRooted must be false when no signals detected")
        } else {
            assertTrue(result.isRooted, "isRooted must be true when signals are present")
        }
    }

    @Test
    fun signals_have_descriptive_text() {
        val result = RootDetector.check()
        result.signals.forEach { signal ->
            assertTrue(signal.isNotBlank(), "Each signal should have descriptive text")
        }
    }

    @Test
    fun clean_test_environment_reports_not_compromised() {
        // A standard CI / developer host (no su/Magisk on Android, no jailbreak artefacts on iOS)
        // must report not rooted/jailbroken.
        val result = RootDetector.check()
        assertFalse(result.isRooted, "Standard test environment should not be flagged as compromised")
        assertTrue(result.signals.isEmpty(), "Signals should be empty in a clean test environment")
    }
}
