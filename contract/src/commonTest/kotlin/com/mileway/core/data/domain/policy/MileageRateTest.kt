package com.mileway.core.data.domain.policy

import com.mileway.core.data.domain.policy.rates.HmrcMileageRates
import com.mileway.core.data.domain.policy.rates.IndiaCarPerquisites
import com.mileway.core.data.domain.policy.rates.IrsMileageRates
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant

class MileageRateTest {
    private val irs = IrsMileageRates.mirror
    private val hmrc = HmrcMileageRates.mirror
    private val india = IndiaCarPerquisites.mirror

    @Test
    fun irsResolvesSubmissionVersionIncludingTheMidyearChange() {
        assertEquals(7000L, irs.amountMinor(100, at("2025-01-01")))
        assertEquals(72_500L, irs.versionFor(at("2026-06-30")).schedule.firstRateThousandthsMinor)
        assertEquals(7250L, irs.amountMinor(100, at("2026-06-30")))
        assertEquals(76_000L, irs.versionFor(at("2026-07-01")).schedule.firstRateThousandthsMinor)
        assertEquals(7600L, irs.amountMinor(100, at("2026-07-01")))
        assertEquals(7250L, irs.amountMinor(100, at("2026-07-01") - 1))
    }

    @Test
    fun hmrcResolvesSubmissionVersionOnSixApril() {
        assertEquals(45_000L, hmrc.versionFor(at("2026-04-05")).schedule.firstRateThousandthsMinor)
        assertEquals(4500L, hmrc.amountMinor(100, at("2026-04-06") - 1))
        assertEquals(55_000L, hmrc.versionFor(at("2026-04-06")).schedule.firstRateThousandthsMinor)
        assertEquals(5500L, hmrc.amountMinor(100, at("2026-04-06")))
    }

    @Test
    fun hmrcSplitsTheTripAcrossTheAnnualBand() {
        val before = AnnualMileageDistance("2026-04-06", 9900)
        assertEquals(8000L, hmrc.amountMinor(200, at("2026-06-01"), before))
        assertEquals(5500L, hmrc.amountMinor(100, at("2026-06-01"), before))
        assertEquals(5000L, hmrc.amountMinor(200, at("2026-06-01"), before.copy(distanceUnits = 10_000)))
        assertEquals(5000L, hmrc.amountMinor(200, at("2026-06-01"), before.copy(distanceUnits = 11_000)))
        assertEquals(0L, hmrc.amountMinor(0, at("2026-06-01"), before))
    }

    @Test
    fun hmrcBusinessDistanceResetsOnSixApril() {
        val priorYear = AnnualMileageDistance("2025-04-06", 10_000)
        assertEquals("2025-04-06", hmrc.annualPeriod.startFor(at("2026-04-06") - 1))
        assertEquals(5000L, hmrc.amountMinor(200, at("2026-04-06") - 1, priorYear))
        assertEquals("2026-04-06", hmrc.annualPeriod.startFor(at("2026-04-06")))
        assertEquals(11_000L, hmrc.amountMinor(200, at("2026-04-06"), priorYear))
        assertEquals("2026-04-06", hmrc.annualPeriod.startFor(at("2027-04-05")))
        assertEquals("2027-04-06", hmrc.annualPeriod.startFor(at("2027-04-06")))
    }

    @Test
    fun futureAndMisalignedAnnualTotalsAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            hmrc.amountMinor(200, at("2026-04-05"), AnnualMileageDistance("2026-04-06", 9900))
        }
        assertFailsWith<IllegalArgumentException> {
            hmrc.amountMinor(200, at("2026-04-06"), AnnualMileageDistance("2026-01-01", 9900))
        }
    }

    @Test
    fun favrAddsEmployerFixedPeriodsToVariableAnnualMileage() {
        val plan = FavrRate(fixedPaymentMinorPerPeriod = 20_000, variableRateThousandthsMinor = 12_500, standardAutomobileCostMinor = 6_170_000)
        assertEquals(365_000L, plan.amountMinor(distanceMiles = 10_000, fixedPeriods = 12, submittedAtMillis = at("2026-06-30")))
        assertEquals(365_000L, plan.amountMinor(distanceMiles = 10_000, fixedPeriods = 12, submittedAtMillis = at("2026-07-01")))
        assertEquals(240_000L, plan.amountMinor(distanceMiles = 0, fixedPeriods = 12, submittedAtMillis = at("2026-01-01")))
        assertEquals(125_000L, plan.amountMinor(distanceMiles = 10_000, fixedPeriods = 0, submittedAtMillis = at("2026-01-01")))
        assertEquals(plan, Json.decodeFromString<FavrRate>(Json.encodeToString(plan)))
    }

    @Test
    fun favrRejectsAnAutomobileAboveTheCapAndUnknownCapYears() {
        val plan = FavrRate(20_000, 12_500, 6_170_001)
        assertFailsWith<IllegalArgumentException> { plan.amountMinor(100, 1, at("2026-01-01")) }
        assertFailsWith<IllegalStateException> { plan.amountMinor(100, 1, at("2025-12-31")) }
        assertFailsWith<IllegalStateException> { plan.amountMinor(100, 1, at("2027-01-01")) }
        assertFailsWith<IllegalArgumentException> { plan.amountMinor(100, 1, at("2026-01-01"), irs.copy(currency = "GBP")) }
    }

    @Test
    fun moneyRoundsHalfUpOnceAfterBandsAndRejectsOverflow() {
        val split = AnnualDistanceStepDown(firstRateThousandthsMinor = 400, firstBandDistanceUnits = 1, aboveBandRateThousandthsMinor = 200)
        assertEquals(1L, split.amountMinor(2))
        assertEquals(0L, AnnualDistanceStepDown(499).amountMinor(1))
        assertEquals(1L, AnnualDistanceStepDown(500).amountMinor(1))
        assertEquals(73L, irs.amountMinor(1, at("2026-01-01")))
        assertEquals(1L, FavrRate(0, 500, 1).amountMinor(1, 0, at("2026-01-01")))
        assertEquals(Long.MAX_VALUE / 1000 + 1, AnnualDistanceStepDown(Long.MAX_VALUE).amountMinor(1))
        assertFailsWith<IllegalArgumentException> { AnnualDistanceStepDown(Long.MAX_VALUE).amountMinor(2) }
        assertFailsWith<IllegalArgumentException> { split.amountMinor(1, Long.MAX_VALUE) }
        val bandOverflow = AnnualDistanceStepDown(Long.MAX_VALUE / 2 + 1, firstBandDistanceUnits = 1, aboveBandRateThousandthsMinor = Long.MAX_VALUE / 2)
        assertFailsWith<IllegalArgumentException> { bandOverflow.amountMinor(3) }
        assertFailsWith<IllegalArgumentException> { FavrRate(Long.MAX_VALUE, 1000, 1).amountMinor(1, 1, at("2026-01-01")) }
        assertFailsWith<IllegalArgumentException> { FavrRate(Long.MAX_VALUE, 0, 1).amountMinor(0, 2, at("2026-01-01")) }
    }

    @Test
    fun schedulesRejectInvalidMoneyDistanceAndBands() {
        assertFailsWith<IllegalArgumentException> { AnnualDistanceStepDown(-1) }
        assertFailsWith<IllegalArgumentException> { AnnualDistanceStepDown(1000, firstBandDistanceUnits = 1) }
        assertFailsWith<IllegalArgumentException> { AnnualDistanceStepDown(1000, firstBandDistanceUnits = 0, aboveBandRateThousandthsMinor = 500) }
        assertFailsWith<IllegalArgumentException> { AnnualDistanceStepDown(1000, firstBandDistanceUnits = 1, aboveBandRateThousandthsMinor = 2000) }
        assertFailsWith<IllegalArgumentException> { AnnualDistanceStepDown(1000).amountMinor(-1) }
        assertFailsWith<IllegalArgumentException> { AnnualDistanceStepDown(1000).amountMinor(1, -1) }
        assertFailsWith<IllegalArgumentException> { AnnualMileageDistance("2026-04-06", -1) }
        assertFailsWith<IllegalArgumentException> { AnnualMileageDistance("2026-02-30", 0) }
        assertFailsWith<IllegalArgumentException> { FavrRate(-1, 1000, 1) }
        assertFailsWith<IllegalArgumentException> { FavrRate(1, -1, 1) }
        assertFailsWith<IllegalArgumentException> { FavrRate(1, 1000, 0) }
        assertFailsWith<IllegalArgumentException> { FavrRate(1, 1000, 1).amountMinor(-1, 1, at("2026-01-01")) }
        assertFailsWith<IllegalArgumentException> { FavrRate(1, 1000, 1).amountMinor(1, -1, at("2026-01-01")) }
    }

    @Test
    fun versionResolutionHandlesUnsortedVersionsAndRejectsUnknownHistory() {
        val unsorted = irs.copy(versions = irs.versions.reversed())
        assertEquals(7250L, unsorted.amountMinor(100, at("2026-06-30")))
        assertFailsWith<IllegalStateException> { irs.versionFor(at("2024-12-31")) }
        assertFailsWith<IllegalStateException> { hmrc.versionFor(at("2025-04-05")) }
        assertFailsWith<IllegalStateException> { india.versionFor(at("2026-03-31")) }
        assertFailsWith<IllegalArgumentException> { irs.copy(versions = irs.versions + irs.versions.first()) }
    }

    @Test
    fun indiaHasOnlyMonthlyPerquisitesAndRetainsTheVerificationLimitation() {
        val rate = india.versionFor(at("2026-04-01"))
        assertEquals("MONTHLY", india.kind)
        assertEquals("INR", india.currency)
        assertEquals(500_000L, rate.amountMinor(1600, false))
        assertEquals(700_000L, rate.amountMinor(1601, false))
        assertEquals(800_000L, rate.amountMinor(1600, true))
        assertEquals(1_000_000L, rate.amountMinor(1601, true))
        assertEquals(
            "secondary sources (Mercans statutory alert; KPMG GMS flash alert 2026-051); gazette text not fetched",
            india.verification,
        )
        assertFailsWith<IllegalArgumentException> { rate.amountMinor(0, false) }
        assertFailsWith<IllegalArgumentException> { india.copy(kind = "PER_DISTANCE") }
    }

    @Test
    fun everyMirrorParsesWithCitationsEffectiveDatesAndSerializablePublicTypes() {
        val mileageMirrors =
            listOf(
                Json.decodeFromString<MileageRateMirror>(IrsMileageRates.json),
                Json.decodeFromString<MileageRateMirror>(HmrcMileageRates.json),
            )
        for (mirror in mileageMirrors) {
            assertTrue(mirror.sourceTitle.isNotBlank())
            assertTrue(mirror.sourceUrl.startsWith("https://"))
            assertEquals("2026-10-04", mirror.retrievedOn)
            assertEquals(mirror, Json.decodeFromString<MileageRateMirror>(Json.encodeToString(mirror)))
            mirror.versions.forEach {
                assertTrue(it.effectiveFrom.isNotBlank())
                assertEquals(MileageDistanceUnit.MILE, it.schedule.distanceUnit)
            }
        }
        assertTrue(india.sourceTitle.isNotBlank())
        assertTrue(india.sourceUrl.startsWith("https://"))
        assertEquals("2026-10-04", india.retrievedOn)
        assertEquals("2026-04-01", india.versions.single().effectiveFrom)
        assertEquals(india, Json.decodeFromString<MonthlyCarPerquisiteMirror>(IndiaCarPerquisites.json))
        assertEquals(india, Json.decodeFromString<MonthlyCarPerquisiteMirror>(Json.encodeToString(india)))
        val cap = irs.favrCostLimits.single()
        assertEquals(6_170_000L, cap.maxStandardAutomobileCostMinor)
        assertEquals("2026-01-01", cap.effectiveFrom)
        assertTrue(cap.sourceTitle.isNotBlank())
        assertEquals("https://www.irs.gov/pub/irs-drop/n-26-10.pdf", cap.sourceUrl)
    }

    private fun at(date: String): Long = Instant.parse("${date}T00:00:00Z").toEpochMilliseconds()
}
