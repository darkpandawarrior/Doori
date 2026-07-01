package com.mileway.feature.tracking.service

import kotlin.test.Test
import kotlin.test.assertEquals

class AutoDiscardCountdownFormatterTest {
    // ── Boundary: zero and negative ───────────────────────────────────────────

    @Test
    fun `zero returns Discarding`() {
        assertEquals("Discarding…", AutoDiscardCountdownFormatter.format(0L))
    }

    @Test
    fun `negative returns Discarding`() {
        assertEquals("Discarding…", AutoDiscardCountdownFormatter.format(-1000L))
    }

    // ── Seconds only (≤ 60 s) ─────────────────────────────────────────────────

    @Test
    fun `exactly 1 second`() {
        assertEquals("1s", AutoDiscardCountdownFormatter.format(1_000L))
    }

    @Test
    fun `59 seconds`() {
        assertEquals("59s", AutoDiscardCountdownFormatter.format(59_000L))
    }

    @Test
    fun `60 seconds transitions to minutes`() {
        assertEquals("1m 0s", AutoDiscardCountdownFormatter.format(60_000L))
    }

    // ── Minutes + seconds ─────────────────────────────────────────────────────

    @Test
    fun `4 minutes 32 seconds`() {
        assertEquals("4m 32s", AutoDiscardCountdownFormatter.format(272_000L))
    }

    @Test
    fun `10 minutes exactly`() {
        assertEquals("10m 0s", AutoDiscardCountdownFormatter.format(600_000L))
    }

    @Test
    fun `sub-second remainder is truncated by floor division`() {
        // 61_500ms = 61s → 1m 1s (the 500ms sub-second is truncated, not rounded)
        assertEquals("1m 1s", AutoDiscardCountdownFormatter.format(61_500L))
    }

    // ── Notification text factory ─────────────────────────────────────────────

    @Test
    fun `notificationText includes countdown`() {
        val text = AutoDiscardCountdownFormatter.notificationText(272_000L)
        assertEquals("Trip will be discarded in 4m 32s", text)
    }

    @Test
    fun `notificationText when expired`() {
        val text = AutoDiscardCountdownFormatter.notificationText(0L)
        assertEquals("Trip will be discarded in Discarding…", text)
    }
}
