package com.mileway.server

import com.mileway.core.data.model.network.ApprovedVehiclePricingResponse
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.PolicyApprovedVehiclesResponse
import com.mileway.core.data.model.network.SubmitMilesRequestK
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V33 B1: exercises the skeleton's only two routes against the H2 in-memory default — no
 * Postgres required. Decodes/encodes with [serverJson] directly (skipping the client-side
 * ContentNegotiation plugin) so the test needs no extra ktor-client dependency beyond the test
 * host already on the classpath.
 */
class ApplicationTest {
    @Test
    fun healthReturns200WithDbOk() =
        testApplication {
            application { module() }

            val response = client.get("/health")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<HealthResponse>(response.bodyAsText())
            assertEquals(HealthResponse(status = "ok", dbOk = true), body)
        }

    @Test
    fun echoRoundTripsSubmitMilesRequestK() =
        testApplication {
            application { module() }

            val original =
                SubmitMilesRequestK(
                    token = "tok-123",
                    vehicleType = "CAR",
                    distance = 12.5,
                    notes = "server echo test",
                )

            val response =
                client.post("/api/echo") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(original))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val decoded = serverJson.decodeFromString<SubmitMilesRequestK>(response.bodyAsText())
            assertEquals(original, decoded)
        }

    @Test
    fun vehiclesReturnsTheSeededDemoList() =
        testApplication {
            application { module() }

            val response = client.get("/api/vehicles")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<PolicyApprovedVehiclesResponse>(response.bodyAsText())
            assertEquals(11, body.vehicles.size)
            val twoWheeler = body.vehicles.single { it.vehicleKey == "twoWheeler" }
            assertEquals("Two Wheeler", twoWheeler.vehicleName)
            assertEquals(16.0, twoWheeler.vehiclePricing)
        }

    @Test
    fun pricingReturnsRatePerKmForEverySeededVehicle() =
        testApplication {
            application { module() }

            val response = client.get("/api/pricing")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<ApprovedVehiclePricingResponse>(response.bodyAsText())
            assertEquals(16.0, body.data["twoWheeler"])
            assertEquals(10.0, body.data["fourWheelerPetrol"])
            assertTrue(body.data.isNotEmpty())
        }

    @Test
    fun submitComputesReimbursementFromThePolicyRateEngine() =
        testApplication {
            application { module() }

            val request =
                SubmitMilesRequestK(
                    token = "tok-submit-1",
                    vehicleType = "twoWheeler",
                    distance = 10.0,
                )

            val response =
                client.post("/api/miles/submit") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(request))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<ExpenseSubmissionResponse>(response.bodyAsText())
            // twoWheeler rate is 16.0 ₹/km (seeded from stub/DemoMockData.kt) × 10km = 160.0, no
            // caps configured — this is the exact PolicyRateEngine.reimbursement(...) output.
            assertEquals(160.0, body.reimbursableAmount)
            assertEquals(160.0, body.amount)
            assertEquals(10.0, body.distance)
            assertTrue(body.transId!!.isNotBlank())
        }
}
