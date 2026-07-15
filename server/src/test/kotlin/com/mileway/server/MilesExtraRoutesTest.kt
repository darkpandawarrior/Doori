package com.mileway.server

import com.mileway.core.data.model.network.CoordsV2
import com.mileway.core.data.model.network.DistanceRequestV2
import com.mileway.core.data.model.network.DistanceResponseV2
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.data.model.network.PostMileageEventRequestK
import com.mileway.core.data.model.network.TrackMileageStatusResponse
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

private const val KM_TOLERANCE = 0.05

/** PLAN_V33 B4: logmiles submit, distance, and track-status routes. */
class MilesExtraRoutesTest {
    @Test
    fun logMilesSubmitComputesReimbursementFromThePolicyRateEngine() =
        testApplication {
            application { module() }

            val response =
                client.post("/api/miles/log") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(LogMilesSubmitRequestV2(vehicleType = "twoWheeler", distance = 5.0)))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<ExpenseSubmissionResponse>(response.bodyAsText())
            // twoWheeler is seeded at 16.0 ₹/km (Schema.kt's seedVehicleRows) × 5km = 80.0.
            assertEquals(80.0, body.reimbursableAmount)
            assertEquals(5.0, body.distance)
        }

    @Test
    fun distanceReturnsTheCorrectKmForAKnownCoordinatePair() =
        testApplication {
            application { module() }

            // One degree of longitude at the equator is ~111.19 km great-circle distance.
            val request = DistanceRequestV2(coords = listOf(CoordsV2(lat = 0.0, lng = 0.0), CoordsV2(lat = 0.0, lng = 1.0)))

            val response =
                client.post("/api/distance") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(request))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<DistanceResponseV2>(response.bodyAsText())
            assertEquals(111.19, body.distance, KM_TOLERANCE)
            assertEquals("km", body.unit)
        }

    @Test
    fun statusReturnsActiveForATokenWithNoHistory() =
        testApplication {
            application { module() }

            val response = client.get("/api/miles/status?token=tok-status-unknown")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<TrackMileageStatusResponse>(response.bodyAsText())
            assertEquals(200, body.statusCode)
            assertEquals(true, body.isActive())
        }

    @Test
    fun statusReflectsTheMostRecentDiscardEvent() =
        testApplication {
            application { module() }

            val token = "tok-status-discarded"
            client.post("/api/miles/discard") {
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(PostMileageEventRequestK(token = token, timestamp = 1_000L)))
            }

            val response = client.get("/api/miles/status?token=$token")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<TrackMileageStatusResponse>(response.bodyAsText())
            assertEquals(504, body.statusCode)
            assertEquals(false, body.isActive())
        }
}
