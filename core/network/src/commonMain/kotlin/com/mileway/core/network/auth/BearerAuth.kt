package com.mileway.core.network.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer

/**
 * PLAN_V34 P2/A6: derives an authenticated client from the toolkit's bare `createHttpClient` output
 * — attaches `Authorization: Bearer <token>` to every request, and on a 401 calls [authApi]'s
 * `/api/auth/refresh` before giving up. This is Ktor's own `Auth`/`bearer{}` plugin, not a hand-rolled
 * retry loop: `createHttpClient` (kmp-toolkit) has no TokenProvider constructor parameter — see its
 * doc comment — so the seam lives here, on top of the client it returns, entirely in Mileway's code.
 *
 * Ktor's bearer provider retries the failed request AT MOST ONCE per 401 (it doesn't loop if the
 * retry also comes back 401), matching the "retry once, then give up" requirement — a second 401
 * propagates as a normal [io.ktor.client.plugins.ResponseException] to the caller.
 *
 * [authApi] must be built on a plain client (no `Auth` installed) — see [AuthApi]'s doc comment.
 */
fun HttpClient.withBearerAuth(
    tokenStore: AuthTokenStore,
    authApi: AuthApi,
): HttpClient =
    config {
        install(Auth) {
            bearer {
                loadTokens {
                    val access = tokenStore.accessToken.value ?: return@loadTokens null
                    BearerTokens(accessToken = access, refreshToken = tokenStore.refreshToken().orEmpty())
                }
                refreshTokens {
                    val newAccess = authApi.refresh() ?: return@refreshTokens null
                    BearerTokens(accessToken = newAccess, refreshToken = tokenStore.refreshToken().orEmpty())
                }
                sendWithoutRequest { request -> !request.url.build().encodedPath.contains("/auth/") }
            }
        }
    }
