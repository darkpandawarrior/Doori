package com.mileway.core.data.domain.claim

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden-gate hardening (backend-validation-matrix.md area D) for the money math this slice
 * actually computes: [Report.totalAmountMinor] and [Report.currency]. Everything else in area D —
 * FX pinning (M01-M04), split-sum-to-anchor (M05/M06), attendee per-head (M07), per-diem generation
 * (M08/M09), advance reconciliation (M10/M11) — needs fields/flows this slice does not implement
 * (no FX rate, no split field, no PerDiemLine/AdvanceLine) and is deferred; see the hardening report.
 *
 * What IS covered: M12 (report total = sum of line amounts) and M13 (no cent created or lost) for
 * the two-subtype, single-currency case this slice ships, plus M04's "integer minor units, no
 * float" half (the type system already forces this — `amountMinor: Long` — these tests are the
 * property proof, not a type-system restatement).
 */
class ReportMoneyMathTest {
    // ── M12: report total = sum of line amounts (after FX — N/A, no FX in this slice) ─────────

    @Test
    fun totalAmountMinorIsTheExactSumOfLineAmounts() {
        val report =
            Report(
                id = "r1",
                employeeId = "emp-1",
                lines =
                    listOf(
                        ExpenseLine(id = "l1", amountMinor = 45_000, currency = "INR", merchant = "Cafe", category = "MEALS"),
                        MileageLine(id = "l2", amountMinor = 12_000, currency = "INR", distanceKm = 12.5, vehicleKey = "twoWheeler"),
                        ExpenseLine(id = "l3", amountMinor = 1, currency = "INR", merchant = "Toll", category = "TRAVEL"),
                    ),
            )

        assertEquals(57_001L, report.totalAmountMinor())
    }

    @Test
    fun emptyReportTotalsToZero() {
        val report = Report(id = "r1", employeeId = "emp-1", lines = emptyList())

        assertEquals(0L, report.totalAmountMinor())
    }

    // ── M13: summing many integer minor-unit amounts creates or loses no cent ──────────────────
    // Property-style: generated inputs, not one example. Long addition is exact (no float in the
    // path), so this is the invariant proof that the design choice (Long, not Double) actually
    // holds under many random amount sets, not just a restatement of the type signature.

    @Test
    fun summingManyRandomLineAmountsNeverDriftsFromTheManualSum() {
        val random = Random(seed = 42)
        repeat(200) { trial ->
            val amounts = List(random.nextInt(1, 30)) { random.nextLong(-1_000_000L, 1_000_000L) }
            val lines =
                amounts.mapIndexed { index, amount ->
                    ExpenseLine(id = "l$index", amountMinor = amount, currency = "INR", merchant = "m", category = "c")
                }
            val report = Report(id = "r-$trial", employeeId = "emp-1", lines = lines)

            val manualSum = amounts.fold(0L) { acc, amount -> acc + amount }
            assertEquals(manualSum, report.totalAmountMinor(), "trial $trial with amounts=$amounts")
        }
    }

    // ── currency(): single-currency-per-report assumption this slice makes ─────────────────────

    @Test
    fun currencyIsTheFirstLinesCurrencyAndDefaultsToInrWhenEmpty() {
        val report =
            Report(
                id = "r1",
                employeeId = "emp-1",
                lines = listOf(MileageLine(id = "l1", amountMinor = 100, currency = "USD", distanceKm = 1.0, vehicleKey = "v")),
            )
        assertEquals("USD", report.currency())

        assertEquals("INR", Report(id = "r2", employeeId = "emp-1", lines = emptyList()).currency())
    }
}
