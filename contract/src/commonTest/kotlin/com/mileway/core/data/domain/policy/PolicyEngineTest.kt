package com.mileway.core.data.domain.policy

import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.Attendee
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.FxRate
import com.mileway.core.data.domain.claim.FxRateSource
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.ledger.PolicyRateTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val DAY_MILLIS = 86_400_000L

class PolicyEngineTest {
    private val v1 =
        PolicyVersion(
            effectiveFrom = 0L,
            rateTable = PolicyRateTable(rates = mapOf("car" to 10.0)),
            maxExpenseAmountMinor = 100_00L,
            receiptRequiredAboveMinor = 50_00L,
        )
    private val v2 =
        PolicyVersion(
            effectiveFrom = 100 * DAY_MILLIS,
            rateTable = PolicyRateTable(rates = mapOf("car" to 20.0), maxReimbursement = 500.0),
            maxExpenseAmountMinor = 200_00L,
            receiptRequiredAboveMinor = 80_00L,
        )
    private val engine = PolicyEngine(listOf(v1, v2))

    @Test
    fun monetaryMessagesUseThePolicyCurrencyAndMajorUnits() {
        val policy = PolicyEngine(listOf(v1.copy(maxExpenseAmountMinor = 2500000, receiptRequiredAboveMinor = 100000)))
        val line = ExpenseLine("over", 2600000, "INR", merchant = "Cafe", category = "FOOD")
        val messages = policy.evaluate(listOf(line), 0).getValue(line.id).associate { it.code to it.message }
        assertEquals("Amount ₹ 26,000.00 exceeds policy max ₹ 25,000.00", messages["EXPENSE_OVER_MAX"])
        assertEquals("Amount ₹ 26,000.00 exceeds ₹ 1,000.00; attach a receipt", messages["RECEIPT_RECOMMENDED"])
        val mileage = MileageLine("mileage", 50000, "INR", distanceKm = 10.0, vehicleKey = "car")
        assertEquals(
            "Claimed ₹ 500.00 exceeds policy-computed ₹ 1.00",
            policy
                .evaluate(listOf(mileage), 0)
                .getValue(mileage.id)
                .single()
                .message,
        )
    }

    @Test
    fun foreignMoneyChecksConvertThroughThePinOrSkipWithAReason() {
        val policy = PolicyEngine(listOf(v1.copy(perHeadLimitMinor = 5000L)))
        val foreign = ExpenseLine("fx", 200L, "USD", merchant = "Cafe", category = "FOOD", attendees = listOf(Attendee("Alex")))
        assertEquals(listOf("FX_POLICY_SKIPPED"), policy.evaluate(listOf(foreign), 0)["fx"]?.map { it.code })
        val pinned = foreign.copy(fxRate = FxRate(90.0, "USD", sourceDate = "2026-09-25"), fxRatePinnedAt = 123)
        val flags = policy.evaluate(listOf(pinned), 0)["fx"].orEmpty().map { it.code }
        assertTrue("EXPENSE_OVER_MAX" in flags)
        assertTrue("EXPENSE_PER_HEAD_OVER_LIMIT" in flags)
        val manual = pinned.copy(fxRate = FxRate(90.0, "USD", source = FxRateSource.MANUAL_APPROXIMATE))
        assertTrue(policy.evaluate(listOf(manual), 0)["fx"].orEmpty().any { it.code == "FX_APPROXIMATE" })
        val wrongPair = pinned.copy(fxRate = pinned.fxRate?.copy(baseCurrency = "EUR"))
        assertEquals("FX_POLICY_SKIPPED", policy.evaluate(listOf(wrongPair), 0)["fx"]?.single()?.code)
    }

    @Test
    fun foreignMileageNeverComparesWithAnInrRateTable() {
        val line = MileageLine("foreign-mileage", 50000, "USD", distanceKm = 10.0, vehicleKey = "car")
        val flags = engine.evaluate(listOf(line), 0)[line.id].orEmpty()
        assertEquals("FX_POLICY_SKIPPED", flags.single().code)
    }

    @Test
    fun `versionFor resolves the version effective at a given date, not the latest`() {
        assertEquals(v1, engine.versionFor(50 * DAY_MILLIS))
        assertEquals(v2, engine.versionFor(150 * DAY_MILLIS))
        assertEquals(v2, engine.versionFor(100 * DAY_MILLIS))
    }

    @Test
    fun `expense over the dated max is a hard block against v1, not v2`() {
        val line = ExpenseLine(id = "e1", amountMinor = 150_00L, currency = "INR", merchant = "m", category = "c")

        val underV1 = engine.evaluate(listOf(line), submittedAtMillis = 50 * DAY_MILLIS)["e1"].orEmpty()
        val underV2 = engine.evaluate(listOf(line), submittedAtMillis = 150 * DAY_MILLIS)["e1"].orEmpty()

        assertTrue(underV1.any { it.code == "EXPENSE_OVER_MAX" && it.severity == PolicySeverity.HARD_BLOCK })
        assertTrue(underV2.none { it.code == "EXPENSE_OVER_MAX" })
    }

    @Test
    fun `expense above the receipt threshold but under the max is a soft warn only`() {
        val line = ExpenseLine(id = "e2", amountMinor = 60_00L, currency = "INR", merchant = "m", category = "c")

        val violations = engine.evaluate(listOf(line), submittedAtMillis = 50 * DAY_MILLIS)["e2"].orEmpty()

        assertEquals(1, violations.size)
        assertEquals(PolicySeverity.SOFT_WARN, violations.single().severity)
        assertEquals("RECEIPT_RECOMMENDED", violations.single().code)
    }

    @Test
    fun `mileage claimed above the dated policy rate is a hard block`() {
        val line = MileageLine(id = "m1", amountMinor = 500_00L, currency = "INR", distanceKm = 10.0, vehicleKey = "car")

        // v1 rate 10.0/km * 10km = 100.0 -> 100 minor units payable; claimed 500 minor is over.
        val violations = engine.evaluate(listOf(line), submittedAtMillis = 50 * DAY_MILLIS)["m1"].orEmpty()

        assertTrue(violations.any { it.code == "MILEAGE_OVER_POLICY_RATE" && it.severity == PolicySeverity.HARD_BLOCK })
    }

    @Test
    fun `mileage capped by the dated table's max reimbursement is a soft warn`() {
        val line = MileageLine(id = "m2", amountMinor = 500L, currency = "INR", distanceKm = 50.0, vehicleKey = "car")

        // v2 rate 20.0/km * 50km = 1000 gross, capped to 500 by v2's maxReimbursement.
        val violations = engine.evaluate(listOf(line), submittedAtMillis = 150 * DAY_MILLIS)["m2"].orEmpty()

        assertTrue(violations.any { it.code == "MILEAGE_RATE_CAPPED" && it.severity == PolicySeverity.SOFT_WARN })
    }

    @Test
    fun `mileage within the policy rate raises no violation`() {
        val line = MileageLine(id = "m3", amountMinor = 50L, currency = "INR", distanceKm = 10.0, vehicleKey = "car")

        val violations = engine.evaluate(listOf(line), submittedAtMillis = 50 * DAY_MILLIS)["m3"].orEmpty()

        assertEquals(emptyList(), violations)
    }

    @Test
    fun `per-head check uses dated limit exact fractions and attendee count`() {
        val policy = PolicyEngine(listOf(v1.copy(perHeadLimitMinor = 500), v2.copy(perHeadLimitMinor = 1000)))
        val line = ExpenseLine("meal", 1001, "INR", merchant = "Cafe", category = "FOOD", attendees = listOf(Attendee("Alex"), Attendee("Jordan")))
        val first = policy.evaluate(listOf(line), 0).getValue("meal").single { it.code == "EXPENSE_PER_HEAD_OVER_LIMIT" }
        assertEquals(PolicySeverity.SOFT_WARN, first.severity)
        assertTrue(policy.evaluate(listOf(line), 150 * DAY_MILLIS).getValue("meal").none { it.code == first.code })
        assertEquals(null, policy.perHeadViolation(1000, 2, 0))
        assertEquals(null, policy.perHeadViolation(1001, 3, 0))
        assertEquals(null, policy.perHeadViolation(1001, 0, 0))
        val huge = PolicyEngine(listOf(v1.copy(perHeadLimitMinor = Long.MAX_VALUE)))
        assertEquals(null, huge.perHeadViolation(Long.MAX_VALUE, 2, 0))
    }

    @Test
    fun `every ClaimLine subtype is evaluated without throwing`() {
        val lines =
            listOf(
                ExpenseLine(id = "e", amountMinor = 10L, currency = "INR", merchant = "m", category = "c"),
                MileageLine(id = "mi", amountMinor = 10L, currency = "INR", distanceKm = 1.0, vehicleKey = "car"),
                PerDiemLine(id = "pd", amountMinor = 10L, currency = "INR", days = 1, dailyRateMinor = 10L),
                AdvanceLine(id = "ad", amountMinor = 10L, currency = "INR", advanceId = "adv1"),
            )

        val result = engine.evaluate(lines, submittedAtMillis = 50 * DAY_MILLIS)

        assertEquals(setOf("e", "mi", "pd", "ad"), result.keys)
    }
}

class PerDiemRateTableTest {
    private val table =
        PerDiemRateTable(
            rates =
                listOf(
                    PerDiemRate(effectiveFrom = 0L, dailyRateMinor = 100_00L),
                    PerDiemRate(effectiveFrom = 100 * DAY_MILLIS, dailyRateMinor = 150_00L),
                ),
        )

    @Test
    fun `rateFor resolves the rate effective at a given date, not the latest`() {
        assertEquals(100_00L, table.rateFor(50 * DAY_MILLIS))
        assertEquals(150_00L, table.rateFor(150 * DAY_MILLIS))
        assertEquals(150_00L, table.rateFor(100 * DAY_MILLIS))
    }

    @Test
    fun `rateFor before the earliest version falls back to the earliest known rate`() {
        assertEquals(100_00L, table.rateFor(-DAY_MILLIS))
    }
}
