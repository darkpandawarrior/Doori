package com.mileway.server

import com.mileway.core.data.model.network.ApprovedVehiclePricingResponse
import com.mileway.core.data.model.network.BulkEventRequestV2
import com.mileway.core.data.model.network.BulkLocationRequestV2
import com.mileway.core.data.model.network.EventPayloadV2
import com.mileway.core.data.model.network.EventResponseV2
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LocationPayloadV2
import com.mileway.core.data.model.network.LocationResponseV2
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

    // ── PLAN_V33 B3: location/event upload — idempotent dedup by opId ────────────

    @Test
    fun locationBatchInsertThenGetReturnsTheRows() =
        testApplication {
            application { module() }

            val token = "tok-loc-range"
            val batch =
                BulkLocationRequestV2(
                    data =
                        listOf(
                            LocationPayloadV2(lat = 1.0, lng = 2.0, token = token, date = 1_000L),
                            LocationPayloadV2(lat = 1.1, lng = 2.1, token = token, date = 2_000L),
                        ),
                )
            val postResponse =
                client.post("/api/location/batch") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(batch))
                }
            assertEquals(HttpStatusCode.OK, postResponse.status)

            val getResponse = client.get("/api/location?token=$token&start=0&end=9999")
            assertEquals(HttpStatusCode.OK, getResponse.status)
            val body = serverJson.decodeFromString<LocationResponseV2>(getResponse.bodyAsText())
            assertEquals(2, body.data.size)
            assertEquals(1_000L, body.data.first().date)
        }

    @Test
    fun locationBatchIsIdempotentWhenReplayedWithTheSameOpId() =
        testApplication {
            application { module() }

            val token = "tok-loc-idempotent"
            val batch =
                BulkLocationRequestV2(
                    data = listOf(LocationPayloadV2(lat = 5.0, lng = 6.0, token = token, date = 500L, opId = "op-fixed-1")),
                )

            repeat(2) {
                val response =
                    client.post("/api/location/batch") {
                        contentType(ContentType.Application.Json)
                        setBody(serverJson.encodeToString(batch))
                    }
                assertEquals(HttpStatusCode.OK, response.status)
            }

            val getResponse = client.get("/api/location?token=$token&start=0&end=9999")
            val body = serverJson.decodeFromString<LocationResponseV2>(getResponse.bodyAsText())
            // One row survives the double-POST replay — insertIgnore no-ops the second insert
            // because op-fixed-1 already exists under the LocationPointsTable.opId unique index.
            assertEquals(1, body.data.size)
        }

    @Test
    fun locationRowsWithNullOpIdAlwaysInsert() =
        testApplication {
            application { module() }

            val token = "tok-loc-null-opid"
            val batch =
                BulkLocationRequestV2(
                    data =
                        listOf(
                            LocationPayloadV2(lat = 7.0, lng = 8.0, token = token, date = 700L, opId = null),
                            LocationPayloadV2(lat = 7.1, lng = 8.1, token = token, date = 710L, opId = null),
                        ),
                )
            client.post("/api/location/batch") {
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(batch))
            }

            val getResponse = client.get("/api/location?token=$token&start=0&end=9999")
            val body = serverJson.decodeFromString<LocationResponseV2>(getResponse.bodyAsText())
            assertEquals(2, body.data.size)
        }

    @Test
    fun eventsBatchInsertThenGetReturnsTheRows() =
        testApplication {
            application { module() }

            val token = "tok-evt-range"
            val batch =
                BulkEventRequestV2(
                    data = listOf(EventPayloadV2(token = token, event = "TRIP_START", time = 100L)),
                )
            client.post("/api/events/batch") {
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(batch))
            }

            val getResponse = client.get("/api/events?token=$token&start=0&end=9999")
            assertEquals(HttpStatusCode.OK, getResponse.status)
            val body = serverJson.decodeFromString<EventResponseV2>(getResponse.bodyAsText())
            assertEquals(1, body.data.size)
            assertEquals("TRIP_START", body.data.first().event)
        }

    @Test
    fun eventsBatchIsIdempotentWhenReplayedWithTheSameOpId() =
        testApplication {
            application { module() }

            val token = "tok-evt-idempotent"
            val batch =
                BulkEventRequestV2(
                    data = listOf(EventPayloadV2(token = token, event = "TRIP_STOP", time = 200L, opId = "op-fixed-evt-1")),
                )

            repeat(2) {
                val response =
                    client.post("/api/events/batch") {
                        contentType(ContentType.Application.Json)
                        setBody(serverJson.encodeToString(batch))
                    }
                assertEquals(HttpStatusCode.OK, response.status)
            }

            val getResponse = client.get("/api/events?token=$token&start=0&end=9999")
            val body = serverJson.decodeFromString<EventResponseV2>(getResponse.bodyAsText())
            assertEquals(1, body.data.size)
        }
}
