package com.mileway.core.network.netlog

import androidx.compose.runtime.Immutable

/**
 * One recorded HTTP exchange. Populated by [NetworkLogPlugin] and rendered by the debug
 * NetworkLogScreen; [toCurl] is the replay affordance (copy-pasteable curl command).
 */
@Immutable
data class NetworkLogEntry(
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val status: Int? = null,
    val responseBody: String? = null,
    val durationMs: Long? = null,
    val timestamp: Long = 0L,
)

/** Renders this entry as a copy-pasteable `curl` command for local replay. */
fun NetworkLogEntry.toCurl(): String =
    buildString {
        append("curl -X ")
        append(method)
        requestHeaders.forEach { (name, value) ->
            append(" -H '")
            append(name)
            append(": ")
            append(value)
            append("'")
        }
        requestBody?.let {
            append(" -d '")
            append(it)
            append("'")
        }
        append(" '")
        append(url)
        append("'")
    }
