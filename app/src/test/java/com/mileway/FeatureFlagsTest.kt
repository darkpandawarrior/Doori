package com.mileway

import com.mileway.core.platform.FeatureFlags
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** CF.1: typed feature-flag reader. */
class FeatureFlagsTest {
    @Test
    fun `typed accessors read the map`() {
        val flags =
            FeatureFlags(
                mapOf(
                    "referralEnabled" to false,
                    "inAppReviewEnabled" to true,
                    "inAppUpdateEnabled" to true,
                ),
            )
        assertFalse(flags.referralEnabled)
        assertTrue(flags.inAppReviewEnabled)
        assertTrue(flags.inAppUpdateEnabled)
    }

    @Test
    fun `defaults apply when a flag is absent`() {
        val flags = FeatureFlags(emptyMap())
        assertTrue(flags.referralEnabled) // default true
        assertTrue(flags.shareEnabled) // default true
        assertFalse(flags.inAppUpdateEnabled) // default false
        assertFalse(flags.isEnabled("unknown"))
        assertTrue(flags.isEnabled("unknown", default = true))
    }
}
