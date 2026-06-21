package com.miletracker

import com.miletracker.stub.DemoMockData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Validates all hardcoded demo data is well-formed.
 */
class DemoMockDataTest {

    @Test
    fun `userConfig is well-formed`() {
        val config = DemoMockData.userConfig()
        assertTrue(config.miles, "miles should be enabled")
        assertTrue(config.logMiles, "logMiles should be enabled")
        assertTrue(config.trackMilesV2, "trackMilesV2 should be enabled")
        assertEquals("INR", config.currency)
        assertNotNull(config.profile)
        assertEquals("demo@miletracker.app", config.profile?.email)
    }

    @Test
    fun `vehicles list is non-empty and has valid pricing`() {
        val response = DemoMockData.vehicles(trackMiles = true)
        assertTrue(response.vehicles.isNotEmpty(), "vehicles must not be empty")
        response.vehicles.forEach { v ->
            assertNotNull(v.vehicleKey, "vehicle key must not be null")
            assertNotNull(v.vehicleName, "vehicle name must not be null")
            val pricing = v.vehiclePricing ?: -1.0
            assertTrue(pricing >= 0, "pricing must be non-negative for ${v.vehicleKey}")
        }
    }

    @Test
    fun `logMilesServices returns expected services`() {
        val services = DemoMockData.logMilesServices().services
        assertNotNull(services)
        assertTrue(services.isNotEmpty(), "services must not be empty")
        services.forEach { s ->
            assertNotNull(s.id)
            assertNotNull(s.name)
        }
    }

    @Test
    fun `submissionResponse calculates correct amount`() {
        val response = DemoMockData.submissionResponse(distanceKm = 10.0)
        assertEquals(1, response.status)
        assertEquals(100.0, response.reimbursableAmount ?: 0.0, 0.01)
        assertEquals(10.0, response.distance, 0.001)
        assertNotNull(response.transId)
        assertTrue(response.transId!!.startsWith("DEMO-TXN-"))
    }

    @Test
    fun `trackMileageStatus is active`() {
        val status = DemoMockData.trackMileageStatus()
        assertEquals(200, status.statusCode)
        assertTrue(status.isActive())
    }
}
