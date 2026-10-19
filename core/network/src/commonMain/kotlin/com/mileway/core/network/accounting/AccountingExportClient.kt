package com.mileway.core.network.accounting

import com.mileway.core.network.api.NetworkBackendFlags
import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One GL-coded claim line. Line-level fields only: no coordinates, route points or addresses. */
@Serializable
data class AccountingExportLine(
    val reportId: String,
    val lineId: String,
    val type: String,
    val categoryKey: String,
    val glAccountCode: String,
    val costCenter: String? = null,
    val amountMinor: Long,
    val currency: String,
)

@Serializable
data class AccountingExportRequest(
    val lines: List<AccountingExportLine>,
)

sealed interface AccountingExportResult {
    data class Accepted(
        val lineCount: Int,
    ) : AccountingExportResult

    /** The server answered with a non-success status. */
    data class Rejected(
        val status: Int,
    ) : AccountingExportResult

    /** Offline, timeout or any other transport failure. */
    data class Unavailable(
        val reason: String,
    ) : AccountingExportResult
}

/**
 * Posts a GL-coded export to an accounting endpoint. Only used when
 * [NetworkBackendFlags.useRealBackend] is true; otherwise the export is a shared CSV. Never throws
 * except for coroutine cancellation.
 */
class AccountingExportClient(
    private val client: HttpClient = createHttpClient(retry = false, requestTimeoutMillis = 5_000),
    private val baseUrl: () -> String = { NetworkBackendFlags.baseUrlOverride ?: DEFAULT_BASE_URL },
) {
    private val json = Json { encodeDefaults = true }

    // Transport failures of every kind map to Unavailable; cancellation is rethrown.
    @Suppress("TooGenericExceptionCaught")
    suspend fun submit(lines: List<AccountingExportLine>): AccountingExportResult {
        if (lines.isEmpty()) return AccountingExportResult.Unavailable("Nothing to export")
        return try {
            val response =
                client.post("${baseUrl().trimEnd('/')}$PATH") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(AccountingExportRequest.serializer(), AccountingExportRequest(lines)))
                }
            if (response.status.isSuccess()) {
                AccountingExportResult.Accepted(lines.size)
            } else {
                AccountingExportResult.Rejected(response.status.value)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            AccountingExportResult.Unavailable(failure.message ?: "Accounting export is unavailable")
        }
    }

    companion object {
        const val PATH = "/api/accounting/export"
        const val DEFAULT_BASE_URL = "http://localhost:8080"
    }
}
