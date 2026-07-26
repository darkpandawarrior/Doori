package com.mileway.server

import com.mileway.core.data.model.network.AllTaggedExpenseResponse
import com.mileway.core.data.model.network.AllTypesResponseV2
import com.mileway.core.data.model.network.CheckInDetailsResponseV2
import com.mileway.core.data.model.network.CheckInRequestV2
import com.mileway.core.data.model.network.SubmittedCheckInResponseV2
import com.mileway.core.data.model.network.SuccessResponseV2
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** PLAN_V33 B4: check-in submit + seeded geo_types. PLAN_V34 P2/B2: every route below needs a bearer token now. */
class CheckInRoutesTest {
    @Test
    fun checkInSubmitReturnsANonNullId() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response =
                client.post("/api/checkin") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(CheckInRequestV2(lat = 18.52, lng = 73.86, typeId = 1L)))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<SuccessResponseV2>(response.bodyAsText())
            assertNotNull(body.id)
        }

    @Test
    fun geoTypesReturnsTheSeededCheckInLocations() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response = client.get("/api/checkin/types") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<AllTypesResponseV2>(response.bodyAsText())
            assertEquals(5, body.types.size)
            val headOffice = body.types.single { it.name == "Head Office" }
            assertEquals(100.0, headOffice.radius)
            assertEquals(18.5204, headOffice.lat)
            assertEquals("OFFICE", headOffice.type)
        }

    @Test
    fun geoTypeByIdReturnsThatSingleSeededRow() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val allTypes =
                serverJson.decodeFromString<AllTypesResponseV2>(
                    client.get("/api/checkin/types") { bearerAuth(token) }.bodyAsText(),
                )
            val firstId = allTypes.types.first().id

            val response = client.get("/api/checkin/types/$firstId") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<CheckInDetailsResponseV2>(response.bodyAsText())
            assertEquals(firstId, body.id)
            assertTrue(body.name.isNotBlank())
        }

    @Test
    fun checkInCenterReturnsANonNullId() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response =
                client.post("/api/checkin/center") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(CheckInRequestV2(lat = 18.55, lng = 73.90, typeId = 2L)))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<SuccessResponseV2>(response.bodyAsText())
            assertNotNull(body.id)
        }

    @Test
    fun submittedCheckInsIncludesARowJustInserted() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val before =
                serverJson.decodeFromString<SubmittedCheckInResponseV2>(
                    client.get("/api/checkin/submitted") { bearerAuth(token) }.bodyAsText(),
                )

            val inserted =
                serverJson.decodeFromString<SuccessResponseV2>(
                    client
                        .post("/api/checkin") {
                            bearerAuth(token)
                            contentType(ContentType.Application.Json)
                            setBody(serverJson.encodeToString(CheckInRequestV2(lat = 1.0, lng = 2.0)))
                        }.bodyAsText(),
                )

            val after =
                serverJson.decodeFromString<SubmittedCheckInResponseV2>(
                    client.get("/api/checkin/submitted") { bearerAuth(token) }.bodyAsText(),
                )

            // H2 is one shared in-memory DB for the whole test JVM — assert the delta, not an
            // absolute total, so this doesn't break when another test class inserts a check-in.
            assertEquals(before.checkIns.size + 1, after.checkIns.size)
            assertTrue(after.checkIns.any { it.id == inserted.id })
        }

    @Test
    fun taggedExpensesReturnsOnlyTheNonPendingSeededRow() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response = client.get("/api/expenses/tagged") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<AllTaggedExpenseResponse>(response.bodyAsText())
            // Only Schema.kt's seedTaggedExpenseRows() feeds this table (pending=false: "Client
            // Lunch") — no other route inserts into it, so an exact-size assertion is safe here.
            assertEquals(1, body.data.size)
            assertEquals("Client Lunch", body.data.single().title)
            assertEquals(450.0, body.data.single().amount)
        }

    @Test
    fun pendingExpensesReturnsOnlyThePendingSeededRow() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            val response = client.get("/api/expenses/pending") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<AllTaggedExpenseResponse>(response.bodyAsText())
            assertEquals(1, body.data.size)
            assertEquals("Taxi Fare", body.data.single().title)
            assertEquals(220.0, body.data.single().amount)
        }

    @Test
    fun pendingExpensesRangeFilterExcludesTheSeededRowWhenTheWindowEndsBeforeIt() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            // seedTaggedExpenseRows' pending row is submitted at 1_700_000_600_000L (Schema.kt) —
            // a window ending just before it must exclude it.
            val response = client.get("/api/expenses/pending?start=0&end=1699999999999") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<AllTaggedExpenseResponse>(response.bodyAsText())
            assertTrue(body.data.isEmpty())
        }

    @Test
    fun pendingExpensesRangeFilterIncludesTheSeededRowOnAnExactBoundaryWindow() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()

            // start == end == the seeded row's submittedAt: greaterEq/lessEq are both inclusive.
            val response = client.get("/api/expenses/pending?start=1700000600000&end=1700000600000") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<AllTaggedExpenseResponse>(response.bodyAsText())
            assertEquals(1, body.data.size)
            assertEquals("Taxi Fare", body.data.single().title)
        }
}
