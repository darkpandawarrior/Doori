package com.mileway

import com.mileway.core.common.deeplink.DeepLinkRouter
import com.mileway.core.common.deeplink.DeepLinkTarget
import com.mileway.core.common.deeplink.DeepLinkValidator
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** DL.1: deep-link router + validator. */
class DeepLinkRouterTest {
    @Test
    fun `custom scheme sections resolve`() {
        assertEquals(DeepLinkTarget.Home, DeepLinkRouter.resolve("mileway://home"))
        assertEquals(DeepLinkTarget.Track, DeepLinkRouter.resolve("mileway://track"))
        assertEquals(DeepLinkTarget.TrackCheckIn, DeepLinkRouter.resolve("mileway://track/checkin"))
        assertEquals(DeepLinkTarget.Log, DeepLinkRouter.resolve("mileway://log"))
        assertEquals(DeepLinkTarget.LogExpense, DeepLinkRouter.resolve("mileway://log/expense"))
        assertEquals(DeepLinkTarget.Profile, DeepLinkRouter.resolve("mileway://profile"))
        assertEquals(DeepLinkTarget.ProfileSettings, DeepLinkRouter.resolve("mileway://profile/settings"))
    }

    @Test
    fun `https app-links path sections resolve`() {
        assertEquals(
            DeepLinkTarget.TrackCheckIn,
            DeepLinkRouter.resolve("https://mileway.example.com/track/checkin"),
        )
        assertEquals(
            DeepLinkTarget.LogExpense,
            DeepLinkRouter.resolve("https://mileway.example.com/log/expense"),
        )
    }

    @Test
    fun `referral parses the code query param`() {
        val target = DeepLinkRouter.resolve("mileway://referral?code=ABC123")
        assertEquals(DeepLinkTarget.Referral("ABC123"), target)
        assertEquals(
            DeepLinkTarget.Referral("XYZ"),
            DeepLinkRouter.resolve("https://mileway.example.com/referral?code=XYZ&utm=foo"),
        )
    }

    @Test
    fun `empty authority resolves to home`() {
        assertEquals(DeepLinkTarget.Home, DeepLinkRouter.resolve("mileway://"))
    }

    @Test
    fun `unknown sections fall through to Unknown`() {
        assertTrue(DeepLinkRouter.resolve("mileway://wat") is DeepLinkTarget.Unknown)
        assertTrue(DeepLinkRouter.resolve("not a uri") is DeepLinkTarget.Unknown)
    }

    @Test
    fun `validator allows custom scheme and the verified https host only`() {
        assertTrue(DeepLinkValidator.isAllowed("mileway://track"))
        assertTrue(DeepLinkValidator.isAllowed("https://mileway.example.com/log"))
        assertFalse(DeepLinkValidator.isAllowed("https://evil.example.com/track"))
        assertFalse(DeepLinkValidator.isAllowed("javascript://alert"))
        assertFalse(DeepLinkValidator.isAllowed("garbage"))
    }
}
