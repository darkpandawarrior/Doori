package com.mileway.core.network.api.impl

import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.core.data.model.network.BulkLocationRequestV2
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LocationPayloadV2
import com.mileway.core.data.model.network.PolicyApprovedVehiclesResponse
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.core.data.model.network.SuccessResponseV2
import com.siddharth.kmp.network.BaseUrlProvider
import com.siddharth.kmp.network.createHttpClient
import com.siddharth.kmp.network.networkJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TEST_BASE_URL = "http://test.local"
private val fixedBaseUrl = BaseUrlProvider { TEST_BASE_URL }
private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

class KtorMilewayNetworkApiTest {
    @Test
    fun getVehicles_hitsVehiclesPathAndDeserializesResponse() =
        runTest {
            var requestedPath = ""
            var requestedMethod: HttpMethod? = null
            val canned = PolicyApprovedVehiclesResponse(vehicles = listOf(ApprovedVehicle(vehicleKey = "CAR", vehicleName = "Car", vehiclePricing = 8.0)))
            val engine =
                MockEngine { request ->
                    requestedPath = request.url.encodedPath
                    requestedMethod = request.method
                    respond(networkJson.encodeToString(canned), HttpStatusCode.OK, jsonHeaders)
                }
            val api = KtorMilewayNetworkApi(createHttpClient(engine = engine, retry = false), fixedBaseUrl)

            val result = api.vehicles(trackMiles = true)

            assertEquals("/api/vehicles", requestedPath)
            assertEquals(HttpMethod.Get, requestedMethod)
            assertEquals(canned, result)
        }

    @Test
    fun submitMiles_postsToMilesSubmitAndDeserializesResponse() =
        runTest {
            var requestedPath = ""
            var requestedMethod: HttpMethod? = null
            val canned = ExpenseSubmissionResponse(amount = 123.0, distance = 10.0, transId = "TXN-1")
            val engine =
                MockEngine { request ->
                    requestedPath = request.url.encodedPath
                    requestedMethod = request.method
                    respond(networkJson.encodeToString(canned), HttpStatusCode.OK, jsonHeaders)
                }
            val api = KtorMilewayNetworkApi(createHttpClient(engine = engine, retry = false), fixedBaseUrl)

            val result = api.submitMiles(SubmitMilesRequestK(token = "t", vehicleType = "CAR", distance = 10.0))

            assertEquals("/api/miles/submit", requestedPath)
            assertEquals(HttpMethod.Post, requestedMethod)
            assertEquals(canned, result)
        }

    @Test
    fun postLocationV2Batch_postsToLocationBatchPath() =
        runTest {
            var requestedPath = ""
            var requestedMethod: HttpMethod? = null
            val engine =
                MockEngine { request ->
                    requestedPath = request.url.encodedPath
                    requestedMethod = request.method
                    respond(networkJson.encodeToString(SuccessResponseV2()), HttpStatusCode.OK, jsonHeaders)
                }
            val api = KtorMilewayNetworkApi(createHttpClient(engine = engine, retry = false), fixedBaseUrl)

            api.postLocationV2Batch(
                BulkLocationRequestV2(data = listOf(LocationPayloadV2(lat = 1.0, lng = 2.0, token = "t", date = 1L, opId = "op-1"))),
            )

            assertEquals("/api/location/batch", requestedPath)
            assertEquals(HttpMethod.Post, requestedMethod)
        }
}
