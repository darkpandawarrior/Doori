package com.mileway.feature.profile.model

import kotlin.test.Test
import kotlin.test.assertEquals

/** PLAN_V22 P6.4: covers [deriveSessionStatus]'s Active/Recent/Idle thresholds. */
class ActiveSessionTest {
    private val now = 1_000_000_000L

    @Test
    fun `a session active within the last 5 minutes is ACTIVE`() {
        assertEquals(SessionStatus.ACTIVE, deriveSessionStatus(lastActiveMillis = now, nowMillis = now))
        assertEquals(SessionStatus.ACTIVE, deriveSessionStatus(lastActiveMillis = now - 4 * 60 * 1_000L, nowMillis = now))
    }

    @Test
    fun `a session active between 5 minutes and 24 hours ago is RECENT`() {
        assertEquals(SessionStatus.RECENT, deriveSessionStatus(lastActiveMillis = now - 10 * 60 * 1_000L, nowMillis = now))
        assertEquals(SessionStatus.RECENT, deriveSessionStatus(lastActiveMillis = now - 23 * 60 * 60 * 1_000L, nowMillis = now))
    }

    @Test
    fun `a session older than 24 hours is IDLE`() {
        assertEquals(SessionStatus.IDLE, deriveSessionStatus(lastActiveMillis = now - 2 * 24 * 60 * 60 * 1_000L, nowMillis = now))
    }

    @Test
    fun `ActiveSession status delegates to deriveSessionStatus`() {
        val session = ActiveSession(id = "S1", deviceName = "Pixel", platform = "Android", lastActiveMillis = now, isCurrent = true)

        assertEquals(SessionStatus.ACTIVE, session.status(now))
    }

    @Test
    fun `deriveDeviceType classifies platform labels`() {
        assertEquals(SessionDeviceType.ANDROID, deriveDeviceType("Android 15"))
        assertEquals(SessionDeviceType.IOS, deriveDeviceType("iPadOS 18"))
        assertEquals(SessionDeviceType.IOS, deriveDeviceType("iPhone 16"))
        assertEquals(SessionDeviceType.WEB, deriveDeviceType("Web"))
        assertEquals(SessionDeviceType.WEB, deriveDeviceType("Chrome on Windows"))
        assertEquals(SessionDeviceType.UNKNOWN, deriveDeviceType("Smart Fridge"))
    }

    @Test
    fun `ActiveSession deviceType derives from platform`() {
        val web = ActiveSession(id = "S2", deviceName = "Chrome", platform = "Web", lastActiveMillis = now, isCurrent = false)

        assertEquals(SessionDeviceType.WEB, web.deviceType)
    }
}
