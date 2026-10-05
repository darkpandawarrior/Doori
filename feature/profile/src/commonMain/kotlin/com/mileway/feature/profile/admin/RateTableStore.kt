package com.mileway.feature.profile.admin

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mileway.core.data.dao.PerDiemRateDao
import com.mileway.core.data.domain.policy.MileageRateMirror
import com.mileway.core.data.domain.policy.MileageRateVersion
import com.mileway.core.data.domain.policy.PerDiemRate
import com.mileway.core.data.domain.policy.PerDiemRateTable
import com.mileway.core.data.domain.policy.PolicyVersion
import com.mileway.core.data.domain.policy.rates.HmrcMileageRates
import com.mileway.core.data.domain.policy.rates.IrsMileageRates
import com.mileway.core.data.ledger.PolicyRateTable
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** Local employer policy rates in INR, separate from the cited government mirrors. */
@Serializable
data class LocalPolicyRateVersion(
    val effectiveFrom: String,
    val ratesMinorPerKm: Map<String, Long>,
) {
    fun policyVersion(): PolicyVersion =
        PolicyVersion(
            effectiveFrom = effectiveDateMillis(effectiveFrom),
            rateTable = PolicyRateTable(ratesMinorPerKm.mapValues { it.value.toDouble() / MINOR_SCALE }),
        )
}

/** A dated per-diem card preserves the original region, grade and currency. */
@Serializable
data class LocalPerDiemRateVersion(
    val region: String,
    val grade: String,
    val currency: String,
    val effectiveFrom: String,
    val dailyRateMinor: Long,
)

@Serializable
data class LocalMileageRates(
    val mirrors: Map<String, MileageRateMirror> = mapOf("IRS" to IrsMileageRates.mirror, "HMRC" to HmrcMileageRates.mirror),
    val policy: List<LocalPolicyRateVersion> = emptyList(),
)

data class RateTables(
    val mileage: LocalMileageRates,
    val perDiem: List<LocalPerDiemRateVersion>,
) {
    fun perDiemTable(
        region: String,
        grade: String,
        currency: String,
    ): PerDiemRateTable =
        PerDiemRateTable(
            perDiem
                .filter { it.region == region && it.grade == grade && it.currency == currency }
                .map { PerDiemRate(effectiveDateMillis(it.effectiveFrom), it.dailyRateMinor) },
        )
}

private const val MINOR_SCALE = 100
private const val ISO_DATE_LENGTH = 10

internal fun effectiveDateMillis(date: String): Long {
    require(date.length == ISO_DATE_LENGTH) { "Enter an effective date as YYYY-MM-DD" }
    val instant = Instant.parse("${date}T00:00:00Z")
    require(instant.toString().substringBefore('T') == date) { "Invalid effective date" }
    return instant.toEpochMilliseconds()
}

/** Exact decimal input conversion. Thousandths of minor units use five places in INR, USD and GBP. */
internal fun decimalRate(
    text: String,
    places: Int = 2,
): Long {
    val value = text.trim()
    require(Regex("[0-9]+(\\.[0-9]{1,$places})?").matches(value)) { "Enter a nonnegative amount with at most $places decimal places" }
    return (value.substringBefore('.') + value.substringAfter('.', "").padEnd(places, '0')).toLongOrNull()
        ?: error("Amount is too large")
}

/**
 * Device-local JSON, seeded once. DataStore transactions reject duplicate/backdated writes without
 * replacing prior versions or overwriting unreadable data. Shared policy consumers are a follow-up.
 */
class RateTableStore(
    private val preferences: DataStore<Preferences>,
    private val perDiemDao: PerDiemRateDao,
) {
    private val json = Json { prettyPrint = true }
    private val mileageKey = stringPreferencesKey("doori_local_mileage_rate_versions")
    private val perDiemKey = stringPreferencesKey("doori_local_per_diem_rate_versions")

    suspend fun read(): RateTables {
        val rows = perDiemDao.observeAll().first()
        val initial =
            rows.map {
                LocalPerDiemRateVersion(
                    it.region,
                    it.grade,
                    it.currency,
                    Instant.fromEpochMilliseconds(it.effectiveFromMs).toString().substringBefore('T'),
                    it.dailyRateMinor,
                )
            }
        val saved =
            preferences.edit { values ->
                if (values[mileageKey] == null) values[mileageKey] = json.encodeToString(LocalMileageRates())
                if (values[perDiemKey] == null) values[perDiemKey] = json.encodeToString(initial)
            }
        return decode(saved)
    }

    suspend fun addMirrorVersion(
        mirrorKey: String,
        version: MileageRateVersion,
    ) {
        preferences.edit { values ->
            val rates = decode(values).mileage
            val mirror = rates.mirrors.getValue(mirrorKey)
            requireLater(version.effectiveFrom, mirror.versions.map { it.effectiveFrom })
            require(
                version.schedule.distanceUnit ==
                    mirror.versions
                        .first()
                        .schedule.distanceUnit,
            ) { "Distance unit must stay unchanged" }
            val updated = mirror.copy(versions = mirror.versions + version)
            values[mileageKey] = json.encodeToString(rates.copy(mirrors = rates.mirrors + (mirrorKey to updated)))
        }
    }

    suspend fun addPolicyVersion(
        date: String,
        vehicle: String,
        rateMinorPerKm: Long,
    ) {
        require(vehicle.isNotBlank() && rateMinorPerKm >= 0) { "Vehicle and nonnegative rate are required" }
        preferences.edit { values ->
            val rates = decode(values).mileage
            requireLater(date, rates.policy.map { it.effectiveFrom })
            val previous =
                rates.policy
                    .maxByOrNull { it.effectiveFrom }
                    ?.ratesMinorPerKm
                    .orEmpty()
            val version = LocalPolicyRateVersion(date, previous + (vehicle.trim() to rateMinorPerKm))
            values[mileageKey] = json.encodeToString(rates.copy(policy = rates.policy + version))
        }
    }

    suspend fun addPerDiemVersion(version: LocalPerDiemRateVersion) {
        require(version.region.isNotBlank() && version.grade.isNotBlank()) { "Region and grade are required" }
        require(version.currency in setOf("INR", "USD", "GBP") && version.dailyRateMinor >= 0) { "Use INR, USD or GBP and a nonnegative rate" }
        preferences.edit { values ->
            val rates = decode(values).perDiem
            val existing = rates.filter { it.region == version.region && it.grade == version.grade }
            require(existing.all { it.currency == version.currency }) { "A card's currency must stay unchanged" }
            requireLater(version.effectiveFrom, existing.map { it.effectiveFrom })
            values[perDiemKey] = json.encodeToString(rates + version)
        }
    }

    suspend fun exportJson(): String {
        val rates = read()
        return "{\"mileage\":${json.encodeToString(rates.mileage)},\"perDiem\":${json.encodeToString(rates.perDiem)}}"
    }

    private fun decode(values: Preferences): RateTables =
        RateTables(
            json.decodeFromString(requireNotNull(values[mileageKey]) { "Load rates before editing" }),
            json.decodeFromString(requireNotNull(values[perDiemKey]) { "Load rates before editing" }),
        )

    private fun requireLater(
        date: String,
        dates: List<String>,
    ) {
        effectiveDateMillis(date)
        require(dates.all { date > it }) { "Choose a date after the latest version; existing versions cannot be replaced" }
    }
}
