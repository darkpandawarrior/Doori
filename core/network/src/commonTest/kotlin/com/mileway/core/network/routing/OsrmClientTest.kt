package com.mileway.core.network.routing

import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class OsrmClientTest {
    private val a = RoutePoint(12.0, 34.0)
    private val b = RoutePoint(13.0, 35.0)

    @Test
    fun routedDistanceUsesGetLongitudeFirstAndMetres() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("/osrm/route/v1/driving/34.0,12.0;35.0,13.0", request.url.encodedPath)
                    assertEquals("false", request.url.parameters["overview"])
                    assertEquals(null, request.headers[HttpHeaders.Authorization])
                    respond(
                        """{"code":"Ok","routes":[{"distance":12345.0,"duration":900}]}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val http = createHttpClient(engine = engine, retry = false)
            try {
                assertEquals(RouteEstimate.Routed(12.345), OsrmClient(OsrmConfiguration("http://route.test/osrm")) { http }.route(a, b))
            } finally {
                http.close()
            }
        }

    @Test
    fun roundTripRoutesActualReturnLeg() =
        runTest {
            val http =
                createHttpClient(
                    engine =
                        MockEngine { request ->
                            assertEquals("/route/v1/driving/34.0,12.0;35.0,13.0;34.0,12.0", request.url.encodedPath)
                            respond(
                                """{"code":"Ok","routes":[{"distance":21000}]}""",
                                HttpStatusCode.OK,
                                headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        },
                    retry = false,
                )
            try {
                assertEquals(RouteEstimate.Routed(21.0), OsrmClient(OsrmConfiguration("http://route.test")) { http }.route(a, b, true))
            } finally {
                http.close()
            }
        }

    @Test
    fun protectedAndUnconfiguredRoutesNeverCreateHttpClient() =
        runTest {
            val config = OsrmConfiguration()
            val client = OsrmClient(config) { error("Must not create HTTP client") }
            assertIs<RouteEstimate.ManualRequired>(client.route(a, b))
            config.baseUrl = "http://route.test"
            assertIs<RouteEstimate.ManualRequired>(client.route(a.copy(protected = true), b))
            assertIs<RouteEstimate.ManualRequired>(client.route(a, b.copy(protected = true), true))
        }

    @Test
    fun noMatchInvalidDistanceAndMalformedResponsesRequireManualEntry() =
        runTest {
            for (json in listOf("""{"code":"NoRoute"}""", """{"code":"Ok","routes":[]}""", """{"code":"Ok","routes":[{"distance":-1}]}""", "not json")) {
                val http =
                    createHttpClient(
                        engine = MockEngine { respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) },
                        retry = false,
                    )
                try {
                    assertIs<RouteEstimate.ManualRequired>(OsrmClient(OsrmConfiguration("http://route.test")) { http }.route(a, b))
                } finally {
                    http.close()
                }
            }
        }

    @Test
    fun offlineAndHttpErrorsRequireManualEntryButCancellationPropagates() =
        runTest {
            val offline = createHttpClient(engine = MockEngine { error("offline") }, retry = false)
            val rejected = createHttpClient(engine = MockEngine { respond("unavailable", HttpStatusCode.ServiceUnavailable) }, retry = false)
            val cancelled = createHttpClient(engine = MockEngine { throw CancellationException("cancelled") }, retry = false)
            try {
                assertIs<RouteEstimate.ManualRequired>(OsrmClient(OsrmConfiguration("http://route.test")) { offline }.route(a, b))
                assertIs<RouteEstimate.ManualRequired>(OsrmClient(OsrmConfiguration("http://route.test")) { rejected }.route(a, b))
                assertFailsWith<CancellationException> { OsrmClient(OsrmConfiguration("http://route.test")) { cancelled }.route(a, b) }
            } finally {
                offline.close()
                rejected.close()
                cancelled.close()
            }
        }

    @Test
    fun invalidConfigurationNeverCreatesHttpClient() = runTest {
        for (base in listOf("route.test", "http://route.test?token=example", "http://user:example@route.test", "ftp://route.test")) {
            val client = OsrmClient(OsrmConfiguration(base)) { error("Invalid server must not create HTTP client") }
            assertIs<RouteEstimate.ManualRequired>(client.route(a, b))
        }
    }

    @Test
    fun coordinateValidationRejectsNonFiniteAndOutOfRangeValues() {
        assertFailsWith<IllegalArgumentException> { RoutePoint(Double.NaN, 0.0) }
        assertFailsWith<IllegalArgumentException> { RoutePoint(91.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { RoutePoint(0.0, 181.0) }
    }
}
