package com.miletracker

import com.miletracker.core.common.deeplink.DeepLinkRouter
import com.miletracker.core.common.deeplink.DeepLinkTarget
import com.miletracker.core.common.deeplink.DeepLinkValidator
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** DL.1: deep-link router + validator. */
class DeepLinkRouterTest {
    @Test
    fun `custom scheme sections resolve`() {
        assertEquals(DeepLinkTarget.Home, DeepLinkRouter.resolve("miletracker://home"))
        assertEquals(DeepLinkTarget.Track, DeepLinkRouter.resolve("miletracker://track"))
        assertEquals(DeepLinkTarget.TrackCheckIn, DeepLinkRouter.resolve("miletracker://track/checkin"))
        assertEquals(DeepLinkTarget.Log, DeepLinkRouter.resolve("miletracker://log"))
        assertEquals(DeepLinkTarget.LogExpense, DeepLinkRouter.resolve("miletracker://log/expense"))
        assertEquals(DeepLinkTarget.Profile, DeepLinkRouter.resolve("miletracker://profile"))
        assertEquals(DeepLinkTarget.ProfileSettings, DeepLinkRouter.resolve("miletracker://profile/settings"))
    }

    @Test
    fun `https app-links path sections resolve`() {
        assertEquals(
            DeepLinkTarget.TrackCheckIn,
            DeepLinkRouter.resolve("https://miletracker.example.com/track/checkin"),
        )
        assertEquals(
            DeepLinkTarget.LogExpense,
            DeepLinkRouter.resolve("https://miletracker.example.com/log/expense"),
        )
    }

    @Test
    fun `referral parses the code query param`() {
        val target = DeepLinkRouter.resolve("miletracker://referral?code=ABC123")
        assertEquals(DeepLinkTarget.Referral("ABC123"), target)
        assertEquals(
            DeepLinkTarget.Referral("XYZ"),
            DeepLinkRouter.resolve("https://miletracker.example.com/referral?code=XYZ&utm=foo"),
        )
    }

    @Test
    fun `empty authority resolves to home`() {
        assertEquals(DeepLinkTarget.Home, DeepLinkRouter.resolve("miletracker://"))
    }

    @Test
    fun `unknown sections fall through to Unknown`() {
        assertTrue(DeepLinkRouter.resolve("miletracker://wat") is DeepLinkTarget.Unknown)
        assertTrue(DeepLinkRouter.resolve("not a uri") is DeepLinkTarget.Unknown)
    }

    @Test
    fun `validator allows custom scheme and the verified https host only`() {
        assertTrue(DeepLinkValidator.isAllowed("miletracker://track"))
        assertTrue(DeepLinkValidator.isAllowed("https://miletracker.example.com/log"))
        assertFalse(DeepLinkValidator.isAllowed("https://evil.example.com/track"))
        assertFalse(DeepLinkValidator.isAllowed("javascript://alert"))
        assertFalse(DeepLinkValidator.isAllowed("garbage"))
    }
}
