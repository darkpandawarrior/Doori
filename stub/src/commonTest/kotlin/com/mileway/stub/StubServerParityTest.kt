package com.mileway.stub

import com.mileway.core.data.ledger.PolicyRateEngine
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.data.model.network.SubmitMilesRequestK
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V33 B6 — flipping `NetworkBackendFlags.useRealBackend` must not change the `/api/pricing` rate
 * map or the submit/log reimbursement AMOUNTS. The server derives both from its seeded `vehicles`
 * table via [PolicyRateEngine]; these lock the stub to the same table and the same engine.
 *
 * Scoped deliberately: the fake is not identical to the server in every field, and does not claim to
 * be. It also returns demo-only garnish the server never emits (`submissionStatus`, `violations`,
 * `issuedVoucher`, `transaction`) and different human-readable `message` strings. Money and rates are
 * the contract; presentation is not.
 *
 * Deliberately *not* asserted: the `DEMO-TXN-` transaction-id prefix, which stays different from
 * the server's `TXN-` on purpose so a demo-backend response is identifiable as demo.
 */
class StubServerParityTest {
    private val api = FakeTrackingNetworkApi()
    private val engine = PolicyRateEngine(DemoMockData.rateTable)

    @Test
    fun `pricing serves the vehicle list's own rates`() =
        runTest {
            val pricing = api.pricing().data
            assertEquals(DemoMockData.rateTable.rates, pricing, "pricing() must serve the shared rate table")
            assertEquals(
                DemoMockData.vehicles().vehicles.size,
                pricing.size,
                "every approved vehicle carries a rate — an entry dropping out means a null key or pricing",
            )
            // Spot-checks against the server's own seed rows (server Schema.kt `seedVehicleRows`).
            assertEquals(10.0, pricing["fourWheelerPetrol"] ?: 0.0, 0.001)
            assertEquals(16.0, pricing["twoWheeler"] ?: 0.0, 0.001)
            assertEquals(0.0, pricing["ownVehicle"] ?: -1.0, 0.001)
        }

    @Test
    fun `submitMiles amount matches the shared rate engine`() =
        runTest {
            val response = api.submitMiles(SubmitMilesRequestK(token = "T-1", vehicleType = "twoWheeler", distance = 12.5))
            val expected = engine.reimbursement("twoWheeler", 12.5).cappedAmount.toDouble()
            assertEquals(200.0, expected, 0.001, "twoWheeler is ₹16/km on both sides: 12.5 km → ₹200")
            assertEquals(expected, response.reimbursableAmount ?: 0.0, 0.001)
            assertEquals(expected, response.amount ?: 0.0, 0.001)
            assertTrue(response.transId!!.startsWith("DEMO-TXN-"), "the demo id prefix is intentional")
        }

    @Test
    fun `logMiles amount matches the shared rate engine`() =
        runTest {
            val response = api.logMiles(LogMilesSubmitRequestV2(vehicleType = "electricCar", distance = 7.25))
            val expected = engine.reimbursement("electricCar", 7.25).cappedAmount.toDouble()
            assertEquals(expected, response.reimbursableAmount ?: 0.0, 0.001)
        }

    @Test
    fun `a missing vehicle type reimburses zero, like the server's NONE fallback`() =
        runTest {
            val response = api.submitMiles(SubmitMilesRequestK(token = "T-2", vehicleType = null, distance = 12.5))
            assertEquals(0.0, response.reimbursableAmount ?: -1.0, 0.001)
        }
}
