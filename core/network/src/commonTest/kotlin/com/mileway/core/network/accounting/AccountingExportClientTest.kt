package com.mileway.core.network.accounting

import com.siddharth.kmp.network.BaseUrlProvider
import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountingExportClientTest {
    @Test
    fun sendsCsvOnceToTheConfiguredBackend() =
        runTest {
            val csv = "report_id,line_id,amount_minor,currency,gl_account_code\r\nr1,l1,100,INR,6100\r\n"
            var requests = 0
            val http =
                createHttpClient(engine = MockEngine { request ->
                    requests++
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("https://accounting.example/api/accounting/exports", request.url.toString())
                    val body = request.body as OutgoingContent.ByteArrayContent
                    assertEquals(csv, body.bytes().decodeToString())
                    assertEquals(ContentType.Text.CSV, body.contentType)
                    respond("", HttpStatusCode.Accepted)
                }, retry = false)
            try {
                val client = AccountingExportClient(BaseUrlProvider { "https://accounting.example/" }, http)
                assertTrue(client.export(csv))
                assertFalse(client.export(" "))
                assertEquals(1, requests)
            } finally {
                http.close()
            }
        }

    @Test
    fun serverFailureAndOfflineReturnFalseWithoutRetry() =
        runTest {
            var requests = 0
            val http = createHttpClient(engine = MockEngine {
                requests++
                respond("missing endpoint", HttpStatusCode.NotFound)
            }, retry = false)
            val offline = createHttpClient(engine = MockEngine { throw IllegalStateException("offline") }, retry = false)
            try {
                assertFalse(AccountingExportClient(BaseUrlProvider { "https://accounting.example" }, http).export("csv"))
                assertEquals(1, requests)
                assertFalse(AccountingExportClient(BaseUrlProvider { "https://accounting.example" }, offline).export("csv"))
            } finally {
                http.close()
                offline.close()
            }
        }

    @Test
    fun cancellationPropagates() =
        runTest {
            val http = createHttpClient(engine = MockEngine { throw CancellationException("cancelled") }, retry = false)
            try {
                assertFailsWith<CancellationException> {
                    AccountingExportClient(BaseUrlProvider { "https://accounting.example" }, http).export("csv")
                }
            } finally {
                http.close()
            }
        }
}
