package com.mileway.feature.whatsnew.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The real branching this task called out: version comparison + requirement derivation. */
class UpdateRequirementTest {
    private val info =
        UpdateInfo(
            latestBuildCode = 100,
            minSupportedBuildCode = 50,
            latestMarketingVersion = "2026.8.36",
            releaseNotes = "Bug fixes and improvements.",
            storeUrl = "https://play.google.com/store/apps/details?id=com.mileway",
        )

    @Test
    fun `current build below the minimum supported build is FORCED`() {
        assertEquals(UpdateUrgency.FORCED, info.deriveRequirement(currentBuildCode = 10).urgency)
        assertEquals(UpdateUrgency.FORCED, info.deriveRequirement(currentBuildCode = 49).urgency)
    }

    @Test
    fun `FORCED wins even when recommended is also true`() {
        val forced = info.copy(recommended = true)
        assertEquals(UpdateUrgency.FORCED, forced.deriveRequirement(currentBuildCode = 10).urgency)
    }

    @Test
    fun `current build at or above latest is NONE`() {
        assertEquals(UpdateUrgency.NONE, info.deriveRequirement(currentBuildCode = 100).urgency)
        assertEquals(UpdateUrgency.NONE, info.deriveRequirement(currentBuildCode = 250).urgency)
    }

    @Test
    fun `current build at exactly the minimum supported build is not FORCED`() {
        assertEquals(UpdateUrgency.OPTIONAL, info.deriveRequirement(currentBuildCode = 50).urgency)
    }

    @Test
    fun `between minimum and latest is OPTIONAL by default`() {
        assertEquals(UpdateUrgency.OPTIONAL, info.deriveRequirement(currentBuildCode = 75).urgency)
    }

    @Test
    fun `between minimum and latest is RECOMMENDED when the remote flag is set`() {
        val recommended = info.copy(recommended = true)
        assertEquals(UpdateUrgency.RECOMMENDED, recommended.deriveRequirement(currentBuildCode = 75).urgency)
    }

    @Test
    fun `recommended flag never turns NONE into RECOMMENDED`() {
        val recommended = info.copy(recommended = true)
        assertEquals(UpdateUrgency.NONE, recommended.deriveRequirement(currentBuildCode = 100).urgency)
    }

    @Test
    fun `derived requirement carries the current build code and the source info`() {
        val requirement = info.deriveRequirement(currentBuildCode = 75)
        assertEquals(75, requirement.currentBuildCode)
        assertEquals(info, requirement.info)
    }

    // ── MarketingVersion: "not naive semver string compare" ──

    @Test
    fun `MarketingVersion parses YYYY dot M dot MILESTONE`() {
        assertEquals(MarketingVersion(2026, 8, 36), MarketingVersion.parse("2026.8.36"))
    }

    @Test
    fun `MarketingVersion parse rejects malformed input`() {
        assertNull(MarketingVersion.parse("2026.8"))
        assertNull(MarketingVersion.parse("2026.8.36.1"))
        assertNull(MarketingVersion.parse("not.a.version"))
        assertNull(MarketingVersion.parse(""))
    }

    @Test
    fun `MarketingVersion compares numerically, not lexicographically`() {
        // A plain string compare gets this backwards: "2026.10.5" < "2026.9.6" lexicographically
        // ('1' < '9'), even though October is after September in the same year.
        val october = MarketingVersion.parse("2026.10.5")!!
        val september = MarketingVersion.parse("2026.9.6")!!
        assertTrue(october > september)
        assertTrue("2026.10.5" < "2026.9.6") // the naive string-compare bug this type exists to avoid
    }

    @Test
    fun `MarketingVersion orders by milestone within the same year and month`() {
        val v36 = MarketingVersion(2026, 8, 36)
        val v37 = MarketingVersion(2026, 8, 37)
        assertTrue(v37 > v36)
    }
}
