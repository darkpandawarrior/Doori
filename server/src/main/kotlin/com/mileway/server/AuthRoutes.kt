package com.mileway.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.mileway.core.data.model.network.AuthRequest
import com.mileway.core.data.model.network.AuthResponse
import com.mileway.core.data.model.network.RefreshRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.security.MessageDigest
import java.util.Date
import java.util.UUID

/**
 * PLAN_V34 P2/B2: the JWT issuer for `authenticate("jwt")` (installed in [configureAuth], wired
 * into `Application.module()` before `routing{}`) plus the two open routes that hand out tokens.
 * Every other route under `/api` lives inside ONE `authenticate("jwt")` block in Application.kt —
 * the routes were built without a token dependency (PLAN_V33.1's AUTH-DEFERRED), so wrapping them
 * needed no restructure.
 *
 * [JWT_ISSUER]/[JWT_AUDIENCE] are internal (not private): AuthRoutesTest builds its own
 * expired/garbage tokens against the same issuer/audience/secret to exercise the 401 path without
 * duplicating these as magic strings.
 */
internal const val JWT_ISSUER = "mileway-server"
internal const val JWT_AUDIENCE = "mileway-client"
private const val JWT_REALM = "mileway"
private const val ACCESS_TOKEN_TTL_MS = 15 * 60 * 1000L
private const val REFRESH_TOKEN_TTL_MS = 30L * 24 * 60 * 60 * 1000L

// ponytail: dev-only fallback secret, clearly marked, never committed as a real secret — a real
// deploy sets JWT_SECRET (see docker-compose.yml); this default only ever backs the local H2 server.
private const val DEV_JWT_SECRET_DEFAULT = "mileway-dev-secret"

internal val jwtAlgorithm: Algorithm
    get() = Algorithm.HMAC256(System.getenv("JWT_SECRET") ?: DEV_JWT_SECRET_DEFAULT)

// internal so ServerPingTable.kt's seedDemoUser() and every test's login helper share one source
// of truth instead of duplicating the demo credential as a second magic string.
internal const val DEMO_EMAIL = "demo@mileway.app"

// ponytail: one hardcoded demo password for the single seeded dev user — this is a portfolio/dev
// server, not a production auth system. Still never stored plaintext (see [sha256] below).
internal const val DEMO_PASSWORD = "mileway-demo-2026"

internal fun sha256(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

fun Application.configureAuth() {
    install(Authentication) {
        jwt("jwt") {
            realm = JWT_REALM
            verifier(
                JWT
                    .require(jwtAlgorithm)
                    .withIssuer(JWT_ISSUER)
                    .withAudience(JWT_AUDIENCE)
                    .build(),
            )
            validate { credential ->
                if (credential.payload.getClaim("email").asString() != null) JWTPrincipal(credential.payload) else null
            }
        }
    }
}

fun Route.authRoutes() {
    post("/api/auth/login") {
        val request = call.receive<AuthRequest>()
        val user =
            transaction {
                UsersTable.selectAll().where { UsersTable.email eq request.email }.firstOrNull()
            }
        if (user == null || user[UsersTable.passwordHash] != sha256(request.password)) {
            call.respond(HttpStatusCode.Unauthorized)
            return@post
        }
        call.respond(issueTokenPair(request.email))
    }

    post("/api/auth/refresh") {
        val request = call.receive<RefreshRequest>()
        val email = consumeRefreshToken(request.refreshToken)
        if (email == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return@post
        }
        call.respond(issueTokenPair(email))
    }
}

private fun issueTokenPair(email: String): AuthResponse {
    val now = System.currentTimeMillis()
    val accessToken =
        JWT.create()
            .withIssuer(JWT_ISSUER)
            .withAudience(JWT_AUDIENCE)
            .withClaim("email", email)
            // Unique per issuance (standard JWT "jti" claim) — without it, two logins/refreshes for
            // the same email within the same millisecond sign byte-identical tokens (same issuer,
            // audience, claim, and expiresAt), which both breaks the "rotation" contract and made
            // AuthRoutesTest's refresh test flaky under H2's zero-latency queries.
            .withJWTId(UUID.randomUUID().toString())
            .withExpiresAt(Date(now + ACCESS_TOKEN_TTL_MS))
            .sign(jwtAlgorithm)
    val refreshToken = UUID.randomUUID().toString()
    transaction {
        RefreshTokensTable.insert {
            it[token] = refreshToken
            it[RefreshTokensTable.email] = email
            it[expiresAt] = now + REFRESH_TOKEN_TTL_MS
        }
    }
    return AuthResponse(accessToken = accessToken, refreshToken = refreshToken, expiresInSeconds = ACCESS_TOKEN_TTL_MS / 1000)
}

/**
 * Validates + rotates in one step: deletes the presented row (single-use) and returns the owning
 * email, or null if the token is unknown/already-consumed/expired.
 */
private fun consumeRefreshToken(token: String): String? =
    transaction {
        val row = RefreshTokensTable.selectAll().where { RefreshTokensTable.token eq token }.firstOrNull()
        if (row == null || row[RefreshTokensTable.expiresAt] < System.currentTimeMillis()) {
            null
        } else {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.token eq token }
            row[RefreshTokensTable.email]
        }
    }
