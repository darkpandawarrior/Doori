package com.mileway

import com.mileway.feature.cards.security.CardSecurityManager
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Q.5: card PIN gate + verification window. */
class CardSecurityManagerTest {
    @Test
    fun `not verified initially`() {
        val mgr = CardSecurityManager(now = { 0L })
        assertFalse(mgr.canSkipVerification())
    }

    @Test
    fun `wrong pin does not start the window`() {
        val mgr = CardSecurityManager(demoPin = "1234", now = { 1000L })
        assertFalse(mgr.verifyPin("0000"))
        assertFalse(mgr.canSkipVerification())
    }

    @Test
    fun `correct pin opens the window`() {
        var clock = 1000L
        val mgr = CardSecurityManager(demoPin = "1234", windowMillis = 5_000L, now = { clock })
        assertTrue(mgr.verifyPin("1234"))
        assertTrue(mgr.canSkipVerification())
        clock += 4_000L
        assertTrue(mgr.canSkipVerification())
        clock += 2_000L
        assertFalse(mgr.canSkipVerification())
    }

    @Test
    fun `reset clears verification`() {
        val mgr = CardSecurityManager(demoPin = "1234", now = { 1000L })
        mgr.verifyPin("1234")
        mgr.reset()
        assertFalse(mgr.canSkipVerification())
    }
}
