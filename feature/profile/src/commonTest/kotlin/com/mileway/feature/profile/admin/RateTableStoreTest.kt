package com.mileway.feature.profile.admin

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.mileway.core.data.dao.PerDiemRateDao
import com.mileway.core.data.domain.policy.AnnualDistanceStepDown
import com.mileway.core.data.domain.policy.MileageRateVersion
import com.mileway.core.data.domain.policy.rates.HmrcMileageRates
import com.mileway.core.data.model.db.PerDiemRateEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class MemoryRatePreferences : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        mutex.withLock { transform(data.value).also { data.value = it } }
}

internal class SeedRateDao : PerDiemRateDao {
    val seed = PerDiemRateEntity("pune_lead", "Pune", "Lead", 12_500, "INR", effectiveDateMillis("2026-01-01"))

    override fun observeAll() = flowOf(listOf(seed))

    override suspend fun get(
        region: String,
        grade: String,
    ) = seed.takeIf { it.region == region && it.grade == grade }

    override suspend fun upsert(entity: PerDiemRateEntity) = error("The editor must not replace Room cards")
}

class RateTableStoreTest {
    @Test
    fun `dated versions survive recreation and preserve cited mirrors and historical prices`() =
        runTest {
            val preferences = MemoryRatePreferences()
            val store = RateTableStore(preferences, SeedRateDao())
            val initial = store.read()
            store.addMirrorVersion("HMRC", MileageRateVersion("2027-04-06", AnnualDistanceStepDown(60_000), "Local employer rate: approved budget"))
            store.addPolicyVersion("2026-10-01", "car", 850)
            store.addPolicyVersion("2027-01-01", "bike", 450)
            store.addPerDiemVersion(LocalPerDiemRateVersion("Pune", "Lead", "INR", "2027-01-01", 15_000))
            val reloaded = RateTableStore(preferences, SeedRateDao()).read()
            val hmrc = reloaded.mileage.mirrors.getValue("HMRC")
            assertEquals(HmrcMileageRates.mirror.sourceUrl, hmrc.sourceUrl)
            assertEquals(
                initial.mileage.mirrors
                    .getValue("HMRC")
                    .versions,
                hmrc.versions.take(2),
            )
            assertEquals(5_500L, hmrc.amountMinor(100, effectiveDateMillis("2026-10-01")))
            assertEquals(6_000L, hmrc.amountMinor(100, effectiveDateMillis("2027-05-01")))
            assertEquals(
                8.5,
                reloaded.mileage.policy
                    .last()
                    .policyVersion()
                    .rateTable
                    .rateFor("car"),
            )
            assertEquals(
                4.5,
                reloaded.mileage.policy
                    .last()
                    .policyVersion()
                    .rateTable
                    .rateFor("bike"),
            )
            val perDiem = reloaded.perDiemTable("Pune", "Lead", "INR")
            assertEquals(12_500L, perDiem.rateFor(effectiveDateMillis("2026-12-01")))
            assertEquals(15_000L, perDiem.rateFor(effectiveDateMillis("2027-01-01")))
            assertTrue(Json.parseToJsonElement(store.exportJson()).toString().contains("approved budget"))
        }

    @Test
    fun `duplicates backdates invalid dates and currency changes leave stored versions intact`() =
        runTest {
            val store = RateTableStore(MemoryRatePreferences(), SeedRateDao())
            store.read()
            store.addPolicyVersion("2026-10-01", "car", 850)
            val before = store.read()
            assertFailsWith<IllegalArgumentException> { store.addPolicyVersion("2026-10-01", "car", 900) }
            assertFailsWith<IllegalArgumentException> { store.addPolicyVersion("2026-09-01", "car", 900) }
            assertFailsWith<IllegalArgumentException> { store.addPolicyVersion("2026-02-30", "car", 900) }
            assertFailsWith<IllegalArgumentException> { store.addPerDiemVersion(LocalPerDiemRateVersion("Pune", "Lead", "USD", "2027-01-01", 900)) }
            assertEquals(before, store.read())
            assertEquals(72_500L, decimalRate("0.725", 5))
            assertEquals(850L, decimalRate("8.50"))
            listOf("-1", "1.001", "NaN", "1e2", "9999999999999999999999").forEach { input ->
                assertTrue(runCatching { decimalRate(input) }.isFailure, input)
            }
        }
}
