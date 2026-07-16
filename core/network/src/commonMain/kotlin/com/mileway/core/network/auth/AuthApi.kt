package com.mileway.core.network.auth

import com.mileway.core.data.model.network.AuthRequest
import com.mileway.core.data.model.network.AuthResponse
import com.mileway.core.data.model.network.RefreshRequest
import com.siddharth.kmp.network.BaseUrlProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * PLAN_V34 P2/A6: the two auth-only server calls (`/api/auth/login`, `/api/auth/refresh`) plus a
 * local-only [logout]. Kept off [com.mileway.core.network.api.MilewayNetworkApi] on purpose — that
 * interface has 30 methods and every fake implementing it, and neither login nor refresh belongs
 * next to the tracking/expense endpoints it models.
 *
 * [client] must NOT be the same instance [withBearerAuth] wraps for [MilewayNetworkApi] calls — this
 * is the plain client `createHttpClient` returns. Login/refresh don't need (and shouldn't trigger) a
 * bearer-token attach, and building [AuthApi] on a bare client sidesteps any reentrancy question
 * about a 401 firing while already inside a 401-triggered refresh.
 */
class AuthApi(
    private val client: HttpClient,
    private val baseUrlProvider: BaseUrlProvider,
    private val tokenStore: AuthTokenStore,
) {
    private suspend fun url(path: String): String = "${baseUrlProvider.baseUrl()}$path"

    suspend fun login(
        email: String,
        password: String,
    ): AuthResponse {
        val response: AuthResponse =
            client
                .post(url("/api/auth/login")) {
                    contentType(ContentType.Application.Json)
                    setBody(AuthRequest(email = email, password = password))
                }.body()
        tokenStore.store(response.accessToken, response.refreshToken)
        return response
    }

    /**
     * Calls `/api/auth/refresh` with the stored refresh token, stores the rotated pair, and returns
     * the new access token. Returns null (and clears both tokens) when there's no refresh token to
     * try, or the server rejects the one that's stored (expired/already-consumed/revoked) — the
     * caller (KtorMilewayNetworkApi's [withBearerAuth]-wrapped client, or a direct caller) then has
     * nothing to retry with and falls back to whatever an unauthenticated response means for it.
     */
    suspend fun refresh(): String? {
        val currentRefreshToken = tokenStore.refreshToken() ?: return null
        return try {
            val response: AuthResponse =
                client
                    .post(url("/api/auth/refresh")) {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest(refreshToken = currentRefreshToken))
                    }.body()
            tokenStore.store(response.accessToken, response.refreshToken)
            response.accessToken
        } catch (e: ResponseException) {
            tokenStore.clear()
            null
        }
    }

    fun logout() = tokenStore.clear()
}
