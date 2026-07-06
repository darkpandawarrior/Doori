package com.mileway.feature.tracking.service

import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeFormatterTest {
    @Test
    fun `under a minute reads just now`() {
        assertEquals("just now", RelativeTimeFormatter.format(timestampMs = 0L, nowMs = 59_000L))
    }

    @Test
    fun `three minutes reads 3 min ago`() {
        assertEquals("3 min ago", RelativeTimeFormatter.format(timestampMs = 0L, nowMs = 180_000L))
    }

    @Test
    fun `two hours reads 2 hr ago`() {
        assertEquals("2 hr ago", RelativeTimeFormatter.format(timestampMs = 0L, nowMs = 2 * 3_600_000L))
    }

    @Test
    fun `two days reads 2 d ago`() {
        assertEquals("2 d ago", RelativeTimeFormatter.format(timestampMs = 0L, nowMs = 2 * 86_400_000L))
    }
}
