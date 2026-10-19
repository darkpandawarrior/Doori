package com.mileway.core.network.accounting

import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountingExportClientTest {
    private val line =
        AccountingExportLine("r1", "l1", "mileage", "MILEAGE", "6100", "CC-1", 1_250, "INR")

    @Test
    fun postsTheCodedLinesAndReportsAcceptance() =
        runTest {
            var body = ""
            val engine =
                MockEngine { request ->
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("api.test", request.url.host)
                    assertEquals(AccountingExportClient.PATH, request.url.encodedPath)
                    body = request.body.toByteArray().decodeToString()
                    respond("{}")
                }
            val http = createHttpClient(engine = engine, retry = false)
            try {
                val result = AccountingExportClient(http) { "https://api.test/" }.submit(listOf(line))
                assertEquals(AccountingExportResult.Accepted(1), result)
                val sent = Json.decodeFromString(AccountingExportRequest.serializer(), body)
                assertEquals(listOf(line), sent.lines)
                assertFalse(body.contains("lat", ignoreCase = true) || body.contains("lng", ignoreCase = true))
            } finally {
                http.close()
            }
        }

    @Test
    fun serverErrorsOfflineAndEmptyInputNeverThrow() =
        runTest {
            val rejecting = createHttpClient(engine = MockEngine { respond("no", HttpStatusCode.UnprocessableEntity) }, retry = false)
            val offline = createHttpClient(engine = MockEngine { throw IllegalStateException("offline") }, retry = false)
            try {
                assertEquals(AccountingExportResult.Rejected(422), AccountingExportClient(rejecting).submit(listOf(line)))
                assertTrue(AccountingExportClient(offline).submit(listOf(line)) is AccountingExportResult.Unavailable)
                assertTrue(AccountingExportClient(offline).submit(emptyList()) is AccountingExportResult.Unavailable)
            } finally {
                rejecting.close()
                offline.close()
            }
        }

    @Test
    fun cancellationPropagates() =
        runTest {
            val cancelled = createHttpClient(engine = MockEngine { throw CancellationException("cancel") }, retry = false)
            try {
                assertFailsWith<CancellationException> { AccountingExportClient(cancelled).submit(listOf(line)) }
            } finally {
                cancelled.close()
            }
        }
}
