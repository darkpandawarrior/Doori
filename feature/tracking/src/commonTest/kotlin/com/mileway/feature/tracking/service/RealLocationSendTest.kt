package com.mileway.feature.tracking.service

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.network.BulkLocationRequestV2
import com.mileway.core.data.outbox.LocationBatch
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
 * PLAN_V33 A4: [realLocationSend] — maps queued [LocationBatch] rows to
 * [MilewayNetworkApi.postLocationV2Batch] and classifies the HTTP outcome into [SendOutcome] per
 * the reference retry policy. Drives real Ktor exceptions through a [MockEngine] (matching
 * KtorMilewayNetworkApiTest's convention) instead of hand-constructing [io.ktor.client.plugins.ClientRequestException],
 * since that type eagerly reads `response.call.request` in its constructor.
 */
class RealLocationSendTest {
    private fun point(id: Long) = LocationData(id = id, activity = "walk", speed = 1f, lat = 1.0, lng = 2.0, token = "t", batteryPercentage = 80.0)

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
            val dao = FakeIdLookupDao(listOf(point(1), point(2)))
            val send = realLocationSend(api, dao)

            val outcome = send(LocationBatch(token = "t", pointIds = listOf(1, 2)))

            assertEquals(SendOutcome.SUCCESS, outcome)
            val sent = networkJson.decodeFromString<BulkLocationRequestV2>(requireNotNull(capturedJson))
            assertEquals(listOf("t:1", "t:2"), sent.data.map { it.opId })
        }

    @Test
    fun `a 409 conflict is a permanent failure`() =
        runTest {
            val dao = FakeIdLookupDao(listOf(point(1)))
            val send = realLocationSend(apiRespondingWith(HttpStatusCode.Conflict), dao)

            assertEquals(SendOutcome.PERMANENT_FAILURE, send(LocationBatch(token = "t", pointIds = listOf(1))))
        }

    @Test
    fun `404 413 and 412 are also permanent failures`() =
        runTest {
            val dao = FakeIdLookupDao(listOf(point(1)))
            for (status in listOf(HttpStatusCode.NotFound, HttpStatusCode.PayloadTooLarge, HttpStatusCode.PreconditionFailed)) {
                val send = realLocationSend(apiRespondingWith(status), dao)
                assertEquals(SendOutcome.PERMANENT_FAILURE, send(LocationBatch(token = "t", pointIds = listOf(1))), "status $status should be permanent")
            }
        }

    @Test
    fun `a 500 server error is retryable, not permanent`() =
        runTest {
            val dao = FakeIdLookupDao(listOf(point(1)))
            val send = realLocationSend(apiRespondingWith(HttpStatusCode.InternalServerError), dao)

            assertEquals(SendOutcome.RETRYABLE_FAILURE, send(LocationBatch(token = "t", pointIds = listOf(1))))
        }

    @Test
    fun `an unnamed 4xx status is retryable, not silently dropped as permanent`() =
        runTest {
            val dao = FakeIdLookupDao(listOf(point(1)))
            val send = realLocationSend(apiRespondingWith(HttpStatusCode.BadRequest), dao)

            assertEquals(SendOutcome.RETRYABLE_FAILURE, send(LocationBatch(token = "t", pointIds = listOf(1))))
        }

    @Test
    fun `rows already purged before send is a no-op SUCCESS so the drain loop still progresses`() =
        runTest {
            var called = false
            val engine =
                MockEngine {
                    called = true
                    respond("{}", HttpStatusCode.OK, jsonHeaders)
                }
            val api = KtorMilewayNetworkApi(createHttpClient(engine = engine, retry = false), fixedBaseUrl)
            val dao = FakeIdLookupDao(emptyList())
            val send = realLocationSend(api, dao)

            val outcome = send(LocationBatch(token = "t", pointIds = listOf(99)))

            assertEquals(SendOutcome.SUCCESS, outcome)
            assertEquals(false, called, "no HTTP call should be made when there are no rows left to send")
        }
}

private class FakeIdLookupDao(rows: List<LocationData>) : LocationDao by UnimplementedLocationDao {
    private val byId = rows.associateBy { it.id }

    override suspend fun getLocationsByIds(ids: List<Long>): List<LocationData> = ids.mapNotNull { byId[it] }
}

/** Delegate target so [FakeIdLookupDao] only needs to override the one method this test exercises. */
private object UnimplementedLocationDao : LocationDao {
    private fun unimplemented(): Nothing = error("not used by RealLocationSendTest")

    override fun getLocationsByToken(token: String) = unimplemented()

    override suspend fun getLocationsByTokenOnce(token: String) = unimplemented()

    override suspend fun getLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ) = unimplemented()

    override suspend fun countLocationsByToken(token: String) = unimplemented()

    override fun getAllLocations() = unimplemented()

    override fun getLocationsByUploadStatus(uploaded: Boolean) = unimplemented()

    override fun getLocationsByActivity(activity: String) = unimplemented()

    override fun getLocationsByDateRange(
        startDate: Long,
        endDate: Long,
    ) = unimplemented()

    override fun getCheckInLocationsByToken(token: String) = unimplemented()

    override fun getAllCheckInPoints() = unimplemented()

    override suspend fun insertLocation(location: LocationData) = unimplemented()

    override suspend fun insertLocations(locations: List<LocationData>) = unimplemented()

    override suspend fun updateLocation(location: LocationData) = unimplemented()

    override suspend fun updateUploadStatus(
        id: Long,
        uploaded: Boolean,
    ) = unimplemented()

    override suspend fun updateUploadStatusByToken(
        token: String,
        uploaded: Boolean,
    ) = unimplemented()

    override suspend fun deleteLocation(location: LocationData) = unimplemented()

    override suspend fun deleteLocationById(id: Long) = unimplemented()

    override suspend fun deleteLocationsByToken(token: String) = unimplemented()

    override suspend fun deleteUploadedLocations(uploadedValue: Boolean) = unimplemented()

    override suspend fun deleteAllLocations() = unimplemented()

    override suspend fun getLocationCount() = unimplemented()

    override suspend fun getUnuploadedLocationCount(uploadedValue: Boolean) = unimplemented()

    override suspend fun getUnsyncedLocationsByToken(token: String) = unimplemented()

    override suspend fun getUnsyncedLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ) = unimplemented()

    override suspend fun getLocationsByIds(ids: List<Long>) = unimplemented()

    override suspend fun markLocationsAsSynced(locationIds: List<Long>) = unimplemented()

    override suspend fun deleteOlderThan(timestamp: Long) = unimplemented()

    override suspend fun getFirstUnsyncedLocationByToken(token: String) = unimplemented()

    override suspend fun getLastLocationByToken(token: String) = unimplemented()
}
