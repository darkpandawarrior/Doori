package com.mileway.core.network.holiday

import com.mileway.core.data.domain.claim.isFxSourceDate
import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A regional holiday is a possible review flag until the reviewer confirms its subdivision. */
@Serializable
data class PublicHoliday(
    val date: String,
    val name: String,
    val countryCode: String,
    val global: Boolean,
    val counties: List<String>? = null,
    val types: List<String>,
)

/** Coverage absence and failed requests must never be mistaken for a working-day observation. */
sealed interface HolidayCheck {
    data class Holiday(val holidays: List<PublicHoliday>) : HolidayCheck

    data object NotHoliday : HolidayCheck

    data object NoData : HolidayCheck

    data object Offline : HolidayCheck

    data object InvalidInput : HolidayCheck
}

/** Keyless Nager.Date GET with a bounded, process-local country/year cache. Failures can retry. */
class NagerDateClient(
    private val client: HttpClient = createHttpClient(retry = false, requestTimeoutMillis = 5_000),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val years = mutableMapOf<String, List<PublicHoliday>?>()

    /** Uses the claim's calendar date, preserves provider dates and never changes claim amounts. */
    suspend fun check(date: String, countryCode: String): HolidayCheck {
        if (!isFxSourceDate(date) || !countryCode.matches(Regex("[A-Z]{2}"))) return HolidayCheck.InvalidInput
        val year = date.take(4)
        val key = "$countryCode/$year"
        return mutex.withLock {
            try {
                if (key !in years) {
                    val response = client.get("https://date.nager.at/api/v3/PublicHolidays/$year/$countryCode")
                    val holidays =
                        when (response.status) {
                            HttpStatusCode.NoContent, HttpStatusCode.NotFound -> null
                            else -> {
                                if (!response.status.isSuccess()) return@withLock HolidayCheck.Offline
                                json.decodeFromString<List<PublicHoliday>>(response.bodyAsText()).also { rows ->
                                    if (rows.any { !isFxSourceDate(it.date) || !it.date.startsWith(year) || it.countryCode != countryCode || it.name.isBlank() }) {
                                        return@withLock HolidayCheck.Offline
                                    }
                                }
                            }
                        }
                    if (years.size >= 16) years.remove(years.keys.first())
                    years[key] = holidays
                }
                val holidays = years[key] ?: return@withLock HolidayCheck.NoData
                val matching = holidays.filter { it.date == date && "Public" in it.types }
                if (matching.isEmpty()) HolidayCheck.NotHoliday else HolidayCheck.Holiday(matching)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                HolidayCheck.Offline
            }
        }
    }
}
