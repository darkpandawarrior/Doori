package com.mileway.server

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
}
