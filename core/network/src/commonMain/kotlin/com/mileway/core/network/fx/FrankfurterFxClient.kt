package com.mileway.core.network.fx

import com.mileway.core.data.domain.claim.FxRate
import com.mileway.core.data.domain.claim.isFxSourceDate
import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Keyless ECB reference GET. Offline, unsupported pairs and invalid responses return no rate. */
class FrankfurterFxClient(
    private val client: HttpClient = createHttpClient(retry = false, requestTimeoutMillis = 5_000),
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Keeps the provider's actual rate date, including the preceding business day on holidays. */
    suspend fun rate(
        base: String,
        quote: String = "INR",
        date: String? = null,
    ): FxRate? {
        if (!base.matches(Regex("[A-Z]{3}")) || !quote.matches(Regex("[A-Z]{3}")) || base == quote) return null
        return try {
            if (date != null && !isFxSourceDate(date)) return null
            val response =
                client.get("https://api.frankfurter.dev/v2/providers/ecb/rate/$base/$quote") {
                    date?.let { parameter("date", it) }
                }
            if (!response.status.isSuccess()) return null
            val body = json.decodeFromString<Response>(response.bodyAsText())
            FxRate(body.rate, body.base, body.quote, body.date).takeIf {
                it.isUsable(base, quote) && (date == null || body.date <= date)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    @Serializable
    private data class Response(
        val date: String,
        val base: String,
        val quote: String,
        val rate: Double,
    )
}
