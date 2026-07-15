package com.mileway.server

import com.mileway.core.data.model.network.AllTypesResponseV2
import com.mileway.core.data.model.network.CheckInDetailsResponseV2
import com.mileway.core.data.model.network.CheckInRequestV2
import com.mileway.core.data.model.network.SuccessResponseV2
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

/** PLAN_V33 B4: check-in submit + seeded geo_types. */
class CheckInRoutesTest {
    @Test
    fun checkInSubmitReturnsANonNullId() =
        testApplication {
            application { module() }

            val response =
                client.post("/api/checkin") {
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

            val response = client.get("/api/checkin/types")

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

            val allTypes =
                serverJson.decodeFromString<AllTypesResponseV2>(client.get("/api/checkin/types").bodyAsText())
            val firstId = allTypes.types.first().id

            val response = client.get("/api/checkin/types/$firstId")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<CheckInDetailsResponseV2>(response.bodyAsText())
            assertEquals(firstId, body.id)
            assertTrue(body.name.isNotBlank())
        }
}
