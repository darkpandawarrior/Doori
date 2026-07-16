package com.mileway.server

import com.mileway.core.data.model.network.AuthRequest
import com.mileway.core.data.model.network.AuthResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * PLAN_V34 P2/B2: every existing route test now needs a bearer token (Application.kt wraps every
 * route under `/api` except `/health` and the `/api/auth` routes in `authenticate("jwt")`) — this
 * logs in the one seeded demo user ([DEMO_EMAIL]/[DEMO_PASSWORD], AuthRoutes.kt) and returns the
 * access token.
 */
suspend fun HttpClient.demoLoginToken(): String {
    val response =
        post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(serverJson.encodeToString(AuthRequest(email = DEMO_EMAIL, password = DEMO_PASSWORD)))
        }
    return serverJson.decodeFromString<AuthResponse>(response.bodyAsText()).accessToken
}
