package com.mileway

import com.siddharth.kmp.appshell.AnalyticsEvent
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** CF.2: AnalyticsEvent self-clamping (openMF pattern: ≤40-char name/key, ≤100-char value, ≤25 params). */
class AnalyticsEventTest {
    @Test
    fun `safeType clamps the event name to 40 chars`() {
        val event = AnalyticsEvent("x".repeat(60))
        assertEquals(40, event.safeType.length)
    }

    @Test
    fun `safeParams clamps key length and value length`() {
        val event =
            AnalyticsEvent(
                type = "evt",
                params = mapOf("k".repeat(60) to "v".repeat(200)),
            )
        val (key, value) = event.safeParams.entries.first()
        assertEquals(40, key.length)
        assertEquals(100, value.length)
    }

    @Test
    fun `safeParams caps the number of params at 25`() {
        val params = (1..40).associate { "key$it" to "value$it" }
        val event = AnalyticsEvent("evt", params)
        assertTrue(event.safeParams.size <= 25)
    }
}
