package com.mileway.feature.tracking.service

import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.core.data.outbox.TripDraft
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.api.impl.KtorMilewayNetworkApi
import com.siddharth.kmp.network.BaseUrlProvider
import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private val fixedBaseUrl = BaseUrlProvider { "http://test.local" }
private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

/**
 * PLAN_V33 A5: [realMilesSubmitSend] — posts a queued [TripDraft] through
 * [MilewayNetworkApi.submitMiles] and classifies the HTTP outcome per the same retry policy as
 * [RealLocationSendTest] (409/404/413/412 permanent, everything else retryable).
 */
class RealMilesSubmitSendTest {
    private fun apiRespondingWith(status: HttpStatusCode): MilewayNetworkApi {
        val engine = MockEngine { respondError(status = status) }
        return KtorMilewayNetworkApi(createHttpClient(engine = engine, retry = false), fixedBaseUrl)
    }

    @Test
    fun `a successful post maps to Success carrying the server response`() =
        runTest {
            val engine = MockEngine { respond("""{"transId":"TXN-1","reimbursableAmount":42.0}""", HttpStatusCode.OK, jsonHeaders) }
            val api = KtorMilewayNetworkApi(createHttpClient(engine = engine, retry = false), fixedBaseUrl)
            val send = realMilesSubmitSend(api)

            val outcome = send(TripDraft("route-1", SubmitMilesRequestK(token = "route-1", distance = 5.0)))

            val success = assertIs<SubmitOutcome.Success>(outcome)
            assertEquals("TXN-1", success.response.transId)
            assertEquals(42.0, success.response.reimbursableAmount)
        }

    @Test
    fun `a 409 conflict is a permanent failure`() =
        runTest {
            val send = realMilesSubmitSend(apiRespondingWith(HttpStatusCode.Conflict))
            assertEquals(SubmitOutcome.PermanentFailure, send(TripDraft("route-1", SubmitMilesRequestK())))
        }

    @Test
    fun `404 413 and 412 are also permanent failures`() =
        runTest {
            for (status in listOf(HttpStatusCode.NotFound, HttpStatusCode.PayloadTooLarge, HttpStatusCode.PreconditionFailed)) {
                val send = realMilesSubmitSend(apiRespondingWith(status))
                assertEquals(SubmitOutcome.PermanentFailure, send(TripDraft("route-1", SubmitMilesRequestK())), "status $status should be permanent")
            }
        }

    @Test
    fun `a 500 server error is retryable, not permanent`() =
        runTest {
            val send = realMilesSubmitSend(apiRespondingWith(HttpStatusCode.InternalServerError))
            assertEquals(SubmitOutcome.RetryableFailure, send(TripDraft("route-1", SubmitMilesRequestK())))
        }

    @Test
    fun `an unnamed 4xx status is retryable, not silently dropped as permanent`() =
        runTest {
            val send = realMilesSubmitSend(apiRespondingWith(HttpStatusCode.BadRequest))
            assertEquals(SubmitOutcome.RetryableFailure, send(TripDraft("route-1", SubmitMilesRequestK())))
        }
}
