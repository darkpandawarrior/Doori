package com.mileway

import com.mileway.core.data.model.network.CoordsV2
import com.mileway.core.data.model.network.DistanceRequestV2
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.data.model.network.PostMileageEventRequestK
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.stub.FakeTrackingNetworkApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests the fake network API produces correct stub responses.
 */
class FakeNetworkApiTest {

    private lateinit var api: FakeTrackingNetworkApi

    @Before
    fun setUp() {
        api = FakeTrackingNetworkApi()
    }

    @Test
    fun `vehicles returns non-empty list`() = runTest {
        val result = api.vehicles(trackMiles = true)
        assertTrue(result.vehicles.isNotEmpty())
    }

    @Test
    fun `logMilesServices returns non-empty services`() = runTest {
        val result = api.fetchLogMilesServices(isInsideTrip = false)
        assertNotNull(result.services)
        assertTrue(result.services!!.isNotEmpty())
    }

    @Test
    fun `submitMiles returns success status`() = runTest {
        val result = api.submitMiles(SubmitMilesRequestK(token = "test-token", distance = 5.0))
        assertEquals(1, result.status)
        assertTrue((result.reimbursableAmount ?: 0.0) > 0.0)
    }

    @Test
    fun `logMiles reimbursable amount equals distance times rate`() = runTest {
        val result = api.logMiles(LogMilesSubmitRequestV2(distance = 12.4))
        assertEquals(12.4, result.distance, 0.001)
        assertEquals(12.4 * 10.0, result.reimbursableAmount ?: 0.0, 0.01)
    }

    @Test
    fun `distance calculates haversine for two valid coords`() = runTest {
        val result = api.distance(
            DistanceRequestV2(
                coords = listOf(
                    CoordsV2(18.5018, 73.8141),
                    CoordsV2(18.5975, 73.7313)
                )
            )
        )
        assertTrue(result.distance > 0, "Expected positive distance, got ${result.distance}")
        assertTrue(result.distance < 20, "Expected < 20km for Pune route, got ${result.distance}")
    }

    @Test
    fun `distance with single coord returns zero`() = runTest {
        val result = api.distance(DistanceRequestV2(coords = listOf(CoordsV2(18.5018, 73.8141))))
        assertEquals(0.0, result.distance)
    }

    @Test
    fun `trackMileageStatus is active`() = runTest {
        val result = api.getTrackMileageStatus("any-token")
        assertEquals(200, result.statusCode)
        assertTrue(result.isActive())
    }

    @Test
    fun `submitMilesEvent and discardMiles are no-ops`() = runTest {
        api.submitMilesEvent(PostMileageEventRequestK(token = "tok"))
        api.discardMiles(PostMileageEventRequestK(token = "tok"))
    }

    @Test
    fun `fetchMap returns correct lat lng`() = runTest {
        val result = api.fetchMap("18.5018", "73.8141")
        assertEquals(18.5018, result.lat)
        assertEquals(73.8141, result.lng)
        assertNotNull(result.address)
    }
}
