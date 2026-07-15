package com.mileway.server

import com.mileway.core.data.model.network.SubmitMilesRequestK
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Mirrors the client's Ktor JSON config exactly (see external/kmp-toolkit
 * network/HttpClientFactory.kt's `networkJson`) so request/response bodies round-trip
 * byte-identically on both sides of the wire.
 */
val serverJson: Json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

@Serializable
data class HealthResponse(
    val status: String = "ok",
    val dbOk: Boolean = true,
)

private const val DEFAULT_PORT = 8080

fun main() {
    embeddedServer(Netty, port = envPort(), module = Application::module).start(wait = true)
}

private fun envPort(): Int = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT

fun Application.module() {
    connectDatabase()

    install(ContentNegotiation) { json(serverJson) }

    routing {
        get("/health") {
            val dbOk = runCatching { transaction { ServerPingTable.selectAll().count() } }.isSuccess
            call.respond(HealthResponse(dbOk = dbOk))
        }
        post("/api/echo") {
            call.respond(call.receive<SubmitMilesRequestK>())
        }
    }
}
