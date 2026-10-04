package com.mileway.feature.logging.perdiem

import com.mileway.core.data.domain.policy.PerDiemRate
import com.mileway.core.data.domain.policy.PerDiemRateTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class BatchDayRangeGeneratorTest {
    private val zone = TimeZone.of("America/New_York")

    private fun rate(date: String, amount: Long) = PerDiemRate(LocalDate.parse(date).atStartOfDayIn(zone).toEpochMilliseconds(), amount)

    private fun generate(start: String, end: String, rates: List<PerDiemRate>) =
        BatchDayRangeGenerator.generate(LocalDate.parse(start), LocalDate.parse(end), PerDiemRateTable(rates), "INR", zone, "batch")

    @Test
    fun inclusiveRangeCrossesDatedRateChangeAndDstWithoutLosingDays() {
        val result = generate("2026-03-07", "2026-03-10", listOf(rate("2026-03-09", 15000), rate("2026-03-07", 10000)))
        assertEquals(listOf("2026-03-07", "2026-03-08", "2026-03-09", "2026-03-10"), result.lines.map { it.incurredOn })
        assertEquals(listOf(10000L, 10000L, 15000L, 15000L), result.lines.map { it.amountMinor })
        assertTrue(result.lines.all { it.days == 1 && it.dailyRateMinor == it.amountMinor })
        assertEquals(4, result.lines.map { it.id }.distinct().size)
        assertTrue(result.skipped.isEmpty())
    }

    @Test
    fun missingRateDaysNeverUseTheTablesEarliestFallbackOrZero() {
        val result = generate("2026-03-07", "2026-03-09", listOf(rate("2026-03-08", 10000)))
        assertEquals(listOf("2026-03-08", "2026-03-09"), result.lines.map { it.incurredOn })
        assertEquals(LocalDate.parse("2026-03-07"), result.skipped.single().date)
        assertEquals("No per-diem rate in effect on this day", result.skipped.single().reason)
        val empty = generate("2026-03-07", "2026-03-07", emptyList())
        assertTrue(empty.lines.isEmpty())
        assertEquals(1, empty.skipped.size)
        val zero = generate("2026-03-07", "2026-03-07", listOf(rate("2026-03-07", 0)))
        assertTrue(zero.lines.isEmpty())
        assertEquals("The daily rate must be positive", zero.skipped.single().reason)
    }

    @Test
    fun validatesReversedRangesAndTheNamedInclusiveCap() {
        assertEquals("End date must be on or after start date", generate("2026-03-08", "2026-03-07", emptyList()).error)
        assertEquals("Choose at most $MaxPerDiemRangeDays days", generate("2026-03-01", "2026-04-01", emptyList()).error)
        val maximum = generate("2026-03-01", "2026-03-31", listOf(rate("2026-03-01", 10000)))
        assertEquals(MaxPerDiemRangeDays, maximum.lines.size)
    }
}
