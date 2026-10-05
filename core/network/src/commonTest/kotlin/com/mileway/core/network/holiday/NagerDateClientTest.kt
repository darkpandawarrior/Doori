package com.mileway.core.network.holiday

import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class NagerDateClientTest {
    @Test
    fun publicDatesAndRegionalScopeArePreservedAndCachedPerCountryYear() =
        runTest {
            var requests = 0
            val http = createHttpClient(engine = MockEngine { request ->
                requests++
                assertEquals("date.nager.at", request.url.host)
                assertEquals("/api/v3/PublicHolidays/2026/GB", request.url.encodedPath)
                respond("""[{"date":"2026-01-02","name":"2 January","countryCode":"GB","global":false,"counties":["GB-SCT"],"types":["Public"],"localName":"ignored"},{"date":"2026-01-03","name":"Observance","countryCode":"GB","global":true,"types":["Observance"]}]""")
            }, retry = false)
            try {
                val client = NagerDateClient(http)
                val result = assertIs<HolidayCheck.Holiday>(client.check("2026-01-02", "GB"))
                assertEquals("2026-01-02", result.holidays.single().date)
                assertEquals(listOf("GB-SCT"), result.holidays.single().counties)
                assertEquals(HolidayCheck.NotHoliday, client.check("2026-01-03", "GB"))
                assertEquals(HolidayCheck.NotHoliday, client.check("2026-01-04", "GB"))
                assertEquals(1, requests)
            } finally {
                http.close()
            }
        }

    @Test
    fun unsupported204And404AreVisibleNoDataAndCacheByCountryAndYear() =
        runTest {
            for (status in listOf(HttpStatusCode.NoContent, HttpStatusCode.NotFound)) {
                var requests = 0
                val http = createHttpClient(engine = MockEngine {
                    requests++
                    respond("", status)
                }, retry = false)
                try {
                    val client = NagerDateClient(http)
                    assertEquals(HolidayCheck.NoData, client.check("2026-01-26", "IN"))
                    assertEquals(HolidayCheck.NoData, client.check("2026-08-15", "IN"))
                    assertEquals(1, requests)
                    assertEquals(HolidayCheck.NoData, client.check("2027-01-26", "IN"))
                    assertEquals(HolidayCheck.NoData, client.check("2026-01-26", "GB"))
                    assertEquals(3, requests)
                } finally {
                    http.close()
                }
            }
        }

    @Test
    fun invalidInputDoesNotCallNetworkAndFailedRequestsCanRetry() =
        runTest {
            var requests = 0
            val http = createHttpClient(engine = MockEngine {
                requests++
                if (requests == 1) throw IllegalStateException("offline")
                respond("[]")
            }, retry = false)
            try {
                val client = NagerDateClient(http)
                assertEquals(HolidayCheck.InvalidInput, client.check("2026-02-30", "GB"))
                assertEquals(HolidayCheck.InvalidInput, client.check("2026-01-01", "../IN"))
                assertEquals(0, requests)
                assertEquals(HolidayCheck.Offline, client.check("2026-01-01", "GB"))
                assertEquals(HolidayCheck.NotHoliday, client.check("2026-01-01", "GB"))
                assertEquals(2, requests)
            } finally {
                http.close()
            }
        }

    @Test
    fun serverErrorsMalformedDatesAndWrongCountryAreUnavailableAndCancellationPropagates() =
        runTest {
            val responses = listOf(
                "{}" to HttpStatusCode.OK,
                """[{"date":"2025-01-01","name":"New Year","countryCode":"GB","global":true,"types":["Public"]}]""" to HttpStatusCode.OK,
                """[{"date":"2026-01-01","name":"New Year","countryCode":"US","global":true,"types":["Public"]}]""" to HttpStatusCode.OK,
                """[{"date":"2026-02-30","name":"Invalid","countryCode":"GB","global":true,"types":["Public"]}]""" to HttpStatusCode.OK,
                "unavailable" to HttpStatusCode.ServiceUnavailable,
            )
            for ((body, status) in responses) {
                val http = createHttpClient(engine = MockEngine { respond(body, status) }, retry = false)
                try {
                    assertEquals(HolidayCheck.Offline, NagerDateClient(http).check("2026-01-01", "GB"))
                } finally {
                    http.close()
                }
            }
            val http = createHttpClient(engine = MockEngine { throw CancellationException("cancel") }, retry = false)
            try {
                assertFailsWith<CancellationException> { NagerDateClient(http).check("2026-01-01", "GB") }
            } finally {
                http.close()
            }
        }
}
