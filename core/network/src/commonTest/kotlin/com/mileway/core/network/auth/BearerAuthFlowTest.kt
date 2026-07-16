package com.mileway.core.network.auth

import com.mileway.core.data.model.network.ApprovedVehiclePricingResponse
import com.mileway.core.data.model.network.AuthResponse
import com.mileway.core.network.api.impl.KtorMilewayNetworkApi
import com.russhwolf.settings.MapSettings
import com.siddharth.kmp.network.BaseUrlProvider
import com.siddharth.kmp.network.createHttpClient
import com.siddharth.kmp.network.networkJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val fixedBaseUrl = BaseUrlProvider { "http://test.local" }
private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

/**
 * PLAN_V34 P2/A6: drives [withBearerAuth]'s 401 -> refresh -> retry-once flow through a single
 * [MockEngine] — mirrors `RealLocationSendTest`'s MockEngine idiom (real Ktor exceptions/retries,
 * not hand-rolled ones).
 */
class BearerAuthFlowTest {
    @Test
    fun aStaleAccessTokenTriggersOneRefreshThenSucceedsOnRetry() =
        runTest {
            val tokenStore = AuthTokenStore(MapSettings())
            tokenStore.store(accessToken = "stale-access", refreshToken = "valid-refresh")
            var vehiclesCallCount = 0
            var refreshCallCount = 0
            val engine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath == "/api/auth/refresh" -> {
                            refreshCallCount++
                            val body = AuthResponse(accessToken = "fresh-access", refreshToken = "fresh-refresh", expiresInSeconds = 900)
                            respond(networkJson.encodeToString(body), HttpStatusCode.OK, jsonHeaders)
                        }
                        vehiclesCallCount == 0 -> {
                            vehiclesCallCount++
                            respondError(HttpStatusCode.Unauthorized)
                        }
                        else -> {
                            vehiclesCallCount++
                            val body = ApprovedVehiclePricingResponse(data = mapOf("CAR" to 8.0))
                            respond(networkJson.encodeToString(body), HttpStatusCode.OK, jsonHeaders)
                        }
                    }
                }
            val authApi = AuthApi(createHttpClient(engine = engine, retry = false), fixedBaseUrl, tokenStore)
            val protectedClient = createHttpClient(engine = engine, retry = false).withBearerAuth(tokenStore, authApi)
            val api = KtorMilewayNetworkApi(protectedClient, fixedBaseUrl)

            val result = api.pricing()

            assertEquals(mapOf("CAR" to 8.0), result.data)
            assertEquals(1, refreshCallCount)
            assertEquals(2, vehiclesCallCount)
            assertEquals("fresh-access", tokenStore.accessToken.value)
            assertEquals("fresh-refresh", tokenStore.refreshToken())
        }

    @Test
    fun refreshRejectionClearsStoredTokens() =
        runTest {
            val tokenStore = AuthTokenStore(MapSettings())
            tokenStore.store(accessToken = "stale-access", refreshToken = "revoked-refresh")
            val engine = MockEngine { respondError(HttpStatusCode.Unauthorized) }
            val authApi = AuthApi(createHttpClient(engine = engine, retry = false), fixedBaseUrl, tokenStore)

            val refreshed = authApi.refresh()

            assertNull(refreshed)
            assertNull(tokenStore.accessToken.value)
            assertNull(tokenStore.refreshToken())
        }

    @Test
    fun refreshWithNoStoredRefreshTokenIsANoOp() =
        runTest {
            val tokenStore = AuthTokenStore(MapSettings())
            val engine = MockEngine { error("no refresh token stored — the client must not call the network") }
            val authApi = AuthApi(createHttpClient(engine = engine, retry = false), fixedBaseUrl, tokenStore)

            val refreshed = authApi.refresh()

            assertNull(refreshed)
        }
}
