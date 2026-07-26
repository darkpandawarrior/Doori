package com.mileway.feature.tracking.service

import com.mileway.core.data.model.db.EventAudience
import com.mileway.core.data.model.db.EventType
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.network.BulkEventRequestV2
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.api.impl.KtorMilewayNetworkApi
import com.siddharth.kmp.network.BaseUrlProvider
import com.siddharth.kmp.network.createHttpClient
import com.siddharth.kmp.network.networkJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private val fixedBaseUrl = BaseUrlProvider { "http://test.local" }
private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

/**
 * Journey telemetry: [realHardwareEventSend] — maps queued [HardwareEventBatch] rows to
 * [MilewayNetworkApi.postBulkEventsV2] and classifies the HTTP outcome into [SendOutcome] with the
 * exact same policy as [RealLocationSendTest]. Drives real Ktor exceptions through a [MockEngine]
 * (matching that test's convention) instead of hand-constructing
 * [io.ktor.client.plugins.ClientRequestException].
 */
class RealHardwareEventSendTest {
    private fun event(id: Long) =
        HardwareEvent(id = id, token = "t", eventType = EventType.TRACKING_STARTED, event = "Tracking Started", audience = EventAudience.USER)

    private fun apiRespondingWith(status: HttpStatusCode): MilewayNetworkApi {
        val engine = MockEngine { respondError(status = status) }
        return KtorMilewayNetworkApi(createHttpClient(engine = engine, retry = false), fixedBaseUrl)
    }

    @Test
    fun `a successful post maps to SUCCESS and populates a deterministic opId per record`() =
        runTest {
            var capturedJson: String? = null
            val engine =
                MockEngine { request ->
                    capturedJson = (request.body as TextContent).text
                    respond("{}", HttpStatusCode.OK, jsonHeaders)
                }
            val api = KtorMilewayNetworkApi(createHttpClient(engine = engine, retry = false), fixedBaseUrl)
            val dao = FakeUnsyncedHardwareEventDao(listOf(event(1), event(2)))
            val send = realHardwareEventSend(api, dao)

            val outcome = send(HardwareEventBatch(token = "t", eventIds = listOf(1, 2)))

            assertEquals(SendOutcome.SUCCESS, outcome)
            val sent = networkJson.decodeFromString<BulkEventRequestV2>(requireNotNull(capturedJson))
            assertEquals(listOf("t:1", "t:2"), sent.data.map { it.opId })
        }

    @Test
    fun `a 409 conflict is a permanent failure`() =
        runTest {
            val dao = FakeUnsyncedHardwareEventDao(listOf(event(1)))
            val send = realHardwareEventSend(apiRespondingWith(HttpStatusCode.Conflict), dao)

            assertEquals(SendOutcome.PERMANENT_FAILURE, send(HardwareEventBatch(token = "t", eventIds = listOf(1))))
        }

    @Test
    fun `404 413 and 412 are also permanent failures`() =
        runTest {
            val dao = FakeUnsyncedHardwareEventDao(listOf(event(1)))
            for (status in listOf(HttpStatusCode.NotFound, HttpStatusCode.PayloadTooLarge, HttpStatusCode.PreconditionFailed)) {
                val send = realHardwareEventSend(apiRespondingWith(status), dao)
                assertEquals(SendOutcome.PERMANENT_FAILURE, send(HardwareEventBatch(token = "t", eventIds = listOf(1))), "status $status should be permanent")
            }
        }

    @Test
    fun `a 500 server error is retryable, not permanent`() =
        runTest {
            val dao = FakeUnsyncedHardwareEventDao(listOf(event(1)))
            val send = realHardwareEventSend(apiRespondingWith(HttpStatusCode.InternalServerError), dao)

            assertEquals(SendOutcome.RETRYABLE_FAILURE, send(HardwareEventBatch(token = "t", eventIds = listOf(1))))
        }

    @Test
    fun `an unnamed 4xx status is retryable, not silently dropped as permanent`() =
        runTest {
            val dao = FakeUnsyncedHardwareEventDao(listOf(event(1)))
            val send = realHardwareEventSend(apiRespondingWith(HttpStatusCode.BadRequest), dao)

            assertEquals(SendOutcome.RETRYABLE_FAILURE, send(HardwareEventBatch(token = "t", eventIds = listOf(1))))
        }

    @Test
    fun `rows already synced before send is a no-op SUCCESS so the drain loop still progresses`() =
        runTest {
            var called = false
            val engine =
                MockEngine {
                    called = true
                    respond("{}", HttpStatusCode.OK, jsonHeaders)
                }
            val api = KtorMilewayNetworkApi(createHttpClient(engine = engine, retry = false), fixedBaseUrl)
            val dao = FakeUnsyncedHardwareEventDao(emptyList())
            val send = realHardwareEventSend(api, dao)

            val outcome = send(HardwareEventBatch(token = "t", eventIds = listOf(99)))

            assertEquals(SendOutcome.SUCCESS, outcome)
            assertEquals(false, called, "no HTTP call should be made when there are no rows left to send")
        }
}
