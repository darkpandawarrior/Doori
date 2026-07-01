package com.mileway

import app.cash.turbine.test
import com.mileway.core.platform.InMemoryReferralStore
import com.mileway.core.platform.LocalReferralManager
import com.mileway.core.platform.ReferralData
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** RF.1: local referral manager. */
class ReferralManagerTest {
    @Test
    fun `my referral code is stable across calls`() =
        runTest {
            var n = 0
            val manager = LocalReferralManager(InMemoryReferralStore()) { "MTCODE${n++}" }
            val first = manager.myReferralCode()
            val second = manager.myReferralCode()
            assertEquals(first, second)
            assertEquals("MTCODE0", first)
        }

    @Test
    fun `redeem succeeds once then is idempotent`() =
        runTest {
            val manager = LocalReferralManager(InMemoryReferralStore())
            assertTrue(manager.redeem("friend1"))
            assertFalse(manager.redeem("friend1"))
            assertFalse(manager.redeem("  "))
        }

    @Test
    fun `redeem normalizes and exposes the pending referral`() =
        runTest {
            val store = InMemoryReferralStore()
            val manager = LocalReferralManager(store)
            manager.pendingReferral().test {
                assertNull(awaitItem())
                manager.redeem("abc123")
                assertEquals(ReferralData(code = "ABC123", source = "manual"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `capture pushes an attribution into the store`() =
        runTest {
            val store = InMemoryReferralStore()
            val manager = LocalReferralManager(store)
            val data = ReferralData(code = "PROMO", source = "install_referrer", campaign = "launch")
            manager.capture(data)
            manager.pendingReferral().test {
                assertEquals(data, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
