package com.mileway.core.network.netlog

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsText
import io.ktor.http.content.TextContent
import io.ktor.util.AttributeKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val StartTimeKey = AttributeKey<Long>("NetworkLogStartTime")
private val RequestBodyKey = AttributeKey<String>("NetworkLogRequestBody")

/**
 * Ktor `HttpClient` plugin: records every request/response into [store] as a [NetworkLogEntry].
 * Install on the app's `HttpClient(...)` via `install(NetworkLogPlugin(store))`. Purely local/
 * in-memory observability — never itself makes a network call.
 *
 * The PascalCase name is on purpose: this is a plugin factory, and Ktor's own plugins
 * (ContentNegotiation, Logging, HttpTimeout) read as types at the install site. ktlint's
 * function-naming rule exempts factory methods but cannot recognise this one.
 */
@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalTime::class)
fun NetworkLogPlugin(store: NetworkLogStore) =
    createClientPlugin("NetworkLogPlugin") {
        onRequest { request, content ->
            request.attributes.put(StartTimeKey, Clock.System.now().toEpochMilliseconds())
            (content as? TextContent)?.let { request.attributes.put(RequestBodyKey, it.text) }
        }
        onResponse { response ->
            val request = response.call.request
            val startTime = request.attributes.getOrNull(StartTimeKey)
            val now = Clock.System.now().toEpochMilliseconds()
            store.record(
                NetworkLogEntry(
                    method = request.method.value,
                    url = request.url.toString(),
                    requestHeaders = request.headers.entries().associate { it.key to it.value.joinToString(",") },
                    requestBody = request.attributes.getOrNull(RequestBodyKey),
                    status = response.status.value,
                    responseBody = runCatching { response.bodyAsText() }.getOrNull(),
                    durationMs = startTime?.let { now - it },
                    timestamp = now,
                ),
            )
        }
    }
