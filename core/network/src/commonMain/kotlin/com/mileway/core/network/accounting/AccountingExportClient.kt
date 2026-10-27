package com.mileway.core.network.accounting

import com.siddharth.kmp.network.BaseUrlProvider
import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import org.koin.dsl.module

/** Sends validated line-level CSV. The accounting endpoint is not implemented by this change. */
class AccountingExportClient(
    private val baseUrlProvider: BaseUrlProvider,
    private val client: HttpClient = createHttpClient(retry = false, requestTimeoutMillis = 5_000),
) {
    @Suppress("TooGenericExceptionCaught")
    suspend fun export(csv: String): Boolean {
        if (csv.isBlank()) return false
        return try {
            client
                .post("${baseUrlProvider.baseUrl().trimEnd('/')}/api/accounting/exports") {
                    contentType(ContentType.Text.CSV)
                    setBody(csv)
                }.status
                .isSuccess()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }
}

/** Keeps toolkit and Ktor constructor types inside the module which declares those dependencies. */
val accountingExportModule =
    module {
        single { AccountingExportClient(get()) }
    }
