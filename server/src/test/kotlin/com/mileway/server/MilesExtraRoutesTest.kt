package com.mileway.server

import com.mileway.core.data.model.network.CoordsV2
import com.mileway.core.data.model.network.DistanceRequestV2
import com.mileway.core.data.model.network.DistanceResponseV2
import com.mileway.core.data.model.network.EmptyRequest
import com.mileway.core.data.model.network.EventResponseV2
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LogMilesRequestV2
import com.mileway.core.data.model.network.LogMilesResponseV2
import com.mileway.core.data.model.network.LogMilesRoutesResponse
import com.mileway.core.data.model.network.LogMilesServicesResponse
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.data.model.network.MapResponse
import com.mileway.core.data.model.network.PostMileageEventRequestK
import com.mileway.core.data.model.network.SuccessResponseV2
import com.mileway.core.data.model.network.TrackMileageStatusResponse
import io.ktor.client.request.bearerAuth
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val KM_TOLERANCE = 0.05

/** PLAN_V33 B4: logmiles submit, distance, and track-status routes. PLAN_V34 P2/B2: every route below needs a bearer token now. */
class MilesExtraRoutesTest {
    @Test
    fun logMilesSubmitComputesReimbursementFromThePolicyRateEngine() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response =
                client.post("/api/miles/log") {
                    bearerAuth(token)
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
            val token = client.demoLoginToken()

            // One degree of longitude at the equator is ~111.19 km great-circle distance.
            val request = DistanceRequestV2(coords = listOf(CoordsV2(lat = 0.0, lng = 0.0), CoordsV2(lat = 0.0, lng = 1.0)))

            val response =
                client.post("/api/distance") {
                    bearerAuth(token)
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
            val token = client.demoLoginToken()

            val response = client.get("/api/miles/status?token=tok-status-unknown") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<TrackMileageStatusResponse>(response.bodyAsText())
            assertEquals(200, body.statusCode)
            assertEquals(true, body.isActive())
        }

    @Test
    fun statusReflectsTheMostRecentDiscardEvent() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val trackingToken = "tok-status-discarded"
            client.post("/api/miles/discard") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(PostMileageEventRequestK(token = trackingToken, timestamp = 1_000L)))
            }

            val response = client.get("/api/miles/status?token=$trackingToken") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<TrackMileageStatusResponse>(response.bodyAsText())
            assertEquals(504, body.statusCode)
            assertEquals(false, body.isActive())
        }

    @Test
    fun mapEchoesTheQueriedCoordinatesIntoTheAddressPlaceholder() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response = client.get("/api/map?lat=18.52&lng=73.86") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<MapResponse>(response.bodyAsText())
            assertEquals(18.52, body.lat)
            assertEquals(73.86, body.lng)
            assertTrue(body.address!!.contains("18.52"))
            assertTrue(body.address!!.contains("73.86"))
        }

    @Test
    fun mapWithNoQueryParamsReturnsNullCoordinates() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response = client.get("/api/map") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<MapResponse>(response.bodyAsText())
            assertNull(body.lat)
            assertNull(body.lng)
        }

    @Test
    fun logMilesServicesReturnsTheSeededServiceList() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response = client.get("/api/log-miles/services") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<LogMilesServicesResponse>(response.bodyAsText())
            // Schema.kt's seedLogMilesServiceRows() is the only writer of this table — 6 fixed rows.
            assertEquals(6, body.services?.size)
            val ownCar = body.services!!.single { it.id == 1L }
            assertEquals("Own Car", ownCar.name)
            assertEquals("CONV-001", ownCar.glCode)
        }

    @Test
    fun logMilesRoutesReturnsAnEmptyLocationListWithNoRoutingDataYet() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response = client.get("/api/log-miles/routes") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<LogMilesRoutesResponse>(response.bodyAsText())
            assertTrue(body.locations.isEmpty())
        }

    @Test
    fun logMilesLimitReturnsTheFixedMonthlyLimit() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response =
                client.post("/api/miles/log/limit") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(LogMilesRequestV2(vehicleType = "twoWheeler", distance = 3.0)))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<LogMilesResponseV2>(response.bodyAsText())
            assertEquals(50.0, body.limit)
            assertEquals("MONTHLY", body.limitPeriod)
        }

    @Test
    fun milesResetReturnsThePathContactIdAsThePermissionId() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response =
                client.post("/api/miles/reset/42") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(EmptyRequest()))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<SuccessResponseV2>(response.bodyAsText())
            assertEquals(42L, body.permissionId)
        }

    @Test
    fun milesResetWithANonNumericContactIdReturnsANullPermissionId() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response =
                client.post("/api/miles/reset/not-a-number") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(EmptyRequest()))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<SuccessResponseV2>(response.bodyAsText())
            assertNull(body.permissionId)
        }

    @Test
    fun milesEventPersistsTheGivenEventTypeAndTagUnderTheirOwnColumns() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val trackingToken = "tok-miles-event-explicit"
            val response =
                client.post("/api/miles/event") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(
                        serverJson.encodeToString(
                            PostMileageEventRequestK(
                                token = trackingToken,
                                eventType = "TRIP_PAUSE",
                                tag = "manual-pause",
                                timestamp = 12_345L,
                                latitude = 1.23,
                                longitude = 4.56,
                            ),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, response.status)

            val getResponse = client.get("/api/events?token=$trackingToken&start=0&end=999999") { bearerAuth(token) }
            val body = serverJson.decodeFromString<EventResponseV2>(getResponse.bodyAsText())
            assertEquals(1, body.data.size)
            val row = body.data.single()
            // insertMilesEventRow stores request.eventType under the `event` column and
            // request.tag under the `eventType` column (MilesExtraRoutes.kt) — verify the mapping,
            // not just that some row landed.
            assertEquals("TRIP_PAUSE", row.event)
            assertEquals("manual-pause", row.eventType)
            assertEquals(12_345L, row.time)
        }

    @Test
    fun milesEventWithNoEventTypeDefaultsToMilesEvent() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val trackingToken = "tok-miles-event-default"
            client.post("/api/miles/event") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(PostMileageEventRequestK(token = trackingToken, timestamp = 1L)))
            }

            val getResponse = client.get("/api/events?token=$trackingToken&start=0&end=999999") { bearerAuth(token) }
            val body = serverJson.decodeFromString<EventResponseV2>(getResponse.bodyAsText())
            assertEquals("MILES_EVENT", body.data.single().event)
        }
}
