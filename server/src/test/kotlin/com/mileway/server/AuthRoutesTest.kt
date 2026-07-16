package com.mileway.server

import com.auth0.jwt.JWT
import com.mileway.core.data.model.network.AuthRequest
import com.mileway.core.data.model.network.AuthResponse
import com.mileway.core.data.model.network.RefreshRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** PLAN_V34 P2/B2: login, refresh, and the authenticate("jwt") guard on a protected route. */
class AuthRoutesTest {
    @Test
    fun loginWithTheDemoCredentialsReturnsATokenPair() =
        testApplication {
            application { module() }

            val response =
                client.post("/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(AuthRequest(email = DEMO_EMAIL, password = DEMO_PASSWORD)))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = serverJson.decodeFromString<AuthResponse>(response.bodyAsText())
            assertEquals(900L, body.expiresInSeconds)
            assertTrue(body.accessToken.isNotBlank())
            assertTrue(body.refreshToken.isNotBlank())
        }

    @Test
    fun loginWithABadPasswordReturns401() =
        testApplication {
            application { module() }

            val response =
                client.post("/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(AuthRequest(email = DEMO_EMAIL, password = "wrong-password")))
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun refreshWithAValidTokenRotatesAndReturnsANewPair() =
        testApplication {
            application { module() }

            val login =
                serverJson.decodeFromString<AuthResponse>(
                    client
                        .post("/api/auth/login") {
                            contentType(ContentType.Application.Json)
                            setBody(serverJson.encodeToString(AuthRequest(email = DEMO_EMAIL, password = DEMO_PASSWORD)))
                        }.bodyAsText(),
                )

            val refreshResponse =
                client.post("/api/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(RefreshRequest(refreshToken = login.refreshToken)))
                }

            assertEquals(HttpStatusCode.OK, refreshResponse.status)
            val rotated = serverJson.decodeFromString<AuthResponse>(refreshResponse.bodyAsText())
            assertNotEquals(login.accessToken, rotated.accessToken)
            assertNotEquals(login.refreshToken, rotated.refreshToken)

            // The rotated (now-consumed) refresh token is single-use — replaying it is rejected.
            val replay =
                client.post("/api/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(RefreshRequest(refreshToken = login.refreshToken)))
                }
            assertEquals(HttpStatusCode.Unauthorized, replay.status)
        }

    @Test
    fun refreshWithAnUnknownTokenReturns401() =
        testApplication {
            application { module() }

            val response =
                client.post("/api/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(RefreshRequest(refreshToken = "never-issued-token")))
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun protectedRouteWithNoTokenReturns401() =
        testApplication {
            application { module() }

            val response = client.get("/api/vehicles")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun protectedRouteWithAnExpiredTokenReturns401() =
        testApplication {
            application { module() }

            val expiredToken =
                JWT
                    .create()
                    .withIssuer(JWT_ISSUER)
                    .withAudience(JWT_AUDIENCE)
                    .withClaim("email", DEMO_EMAIL)
                    .withExpiresAt(Date(System.currentTimeMillis() - 1_000L))
                    .sign(jwtAlgorithm)

            val response = client.get("/api/vehicles") { bearerAuth(expiredToken) }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
