package com.mileway.core.data.ledger

import com.mileway.core.data.model.network.ApprovedVehicle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PolicyRateEngineTest {
    private val table =
        PolicyRateTable(
            rates = mapOf("car" to 12.0, "bike" to 5.0),
            defaultRatePerKm = 2.0,
        )

    @Test
    fun `exact rate lookup uses the vehicle's own per-km rate`() {
        val result = PolicyRateEngine(table).reimbursement("car", distanceKm = 10.0)

        assertEquals(12.0, result.ratePerKm)
        assertEquals(120.0, result.gross)
        assertEquals(120L, result.cappedAmount)
        assertNull(result.appliedCapReason)
    }

    @Test
    fun `unknown vehicle key falls back to the table default rate`() {
        val result = PolicyRateEngine(table).reimbursement("scooter", distanceKm = 10.0)

        assertEquals(2.0, result.ratePerKm)
        assertEquals(20.0, result.gross)
        assertEquals(20L, result.cappedAmount)
    }

    @Test
    fun `zero distance short-circuits to a zero result without applying caps`() {
        val result = PolicyRateEngine(table).reimbursement("car", distanceKm = 0.0)

        assertEquals(0.0, result.gross)
        assertEquals(0L, result.cappedAmount)
        assertNull(result.appliedCapReason)
    }

    @Test
    fun `negative distance is guarded the same as zero`() {
        val result = PolicyRateEngine(table).reimbursement("car", distanceKm = -5.0)

        assertEquals(0.0, result.gross)
        assertEquals(0L, result.cappedAmount)
    }

    @Test
    fun `min cap raises a below-floor reimbursement to the policy minimum`() {
        val floored = table.copy(minReimbursement = 50.0)

        val result = PolicyRateEngine(floored).reimbursement("bike", distanceKm = 1.0)

        assertEquals(5.0, result.gross)
        assertEquals(50L, result.cappedAmount)
        assertEquals(CapReason.MIN, result.appliedCapReason)
    }

    @Test
    fun `max cap lowers an over-ceiling reimbursement to the policy maximum`() {
        val capped = table.copy(maxReimbursement = 100.0)

        val result = PolicyRateEngine(capped).reimbursement("car", distanceKm = 50.0)

        assertEquals(600.0, result.gross)
        assertEquals(100L, result.cappedAmount)
        assertEquals(CapReason.MAX, result.appliedCapReason)
    }

    @Test
    fun `rounding boundary rounds half-up to the nearest whole minor unit`() {
        val half = PolicyRateTable(rates = mapOf("car" to 0.5))

        val result = PolicyRateEngine(half).reimbursement("car", distanceKm = 1.0)

        assertEquals(0.5, result.gross)
        assertEquals(1L, result.cappedAmount)
    }

    @Test
    fun `fromApprovedVehicles builds a table skipping vehicles with null key or pricing`() {
        val vehicles =
            listOf(
                ApprovedVehicle(vehicleKey = "car", vehicleName = "Car", vehiclePricing = 12.0),
                ApprovedVehicle(vehicleKey = null, vehicleName = "Bad", vehiclePricing = 9.0),
                ApprovedVehicle(vehicleKey = "van", vehicleName = "Van", vehiclePricing = null),
            )

        val built = PolicyRateTable.fromApprovedVehicles(vehicles, defaultRatePerKm = 1.0)

        assertEquals(12.0, built.rateFor("car"))
        assertEquals(1.0, built.rateFor("van"))
        assertEquals(1.0, built.rateFor("missing"))
    }
}
