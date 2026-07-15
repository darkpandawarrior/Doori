package com.mileway.feature.tracking.repository

import com.mileway.core.data.model.network.ApprovedVehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * PLAN_V33 A6: the tiny persisted read-cache behind [VehiclePricingRepository]'s offline-capable
 * vehicles stream. Mirrors core/data's `SnapshotCache` DataStore-blob idiom (one JSON string in a
 * single preference key) — see `com.mileway.core.data.watch.SnapshotCache` — rather than a Room
 * table, since a full vehicle+pricing snapshot is small and has no query/relational needs.
 */
interface VehiclePricingCache {
    val snapshot: Flow<VehiclePricingSnapshot?>

    suspend fun write(snapshot: VehiclePricingSnapshot)
}

@Serializable
data class VehiclePricingSnapshot(
    val vehicles: List<ApprovedVehicle> = emptyList(),
    val pricing: Map<String, Double> = emptyMap(),
)

/** Codec factored out for unit testing without a platform-backed DataStore/NSUserDefaults. */
object VehiclePricingCacheCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(value: VehiclePricingSnapshot): String = json.encodeToString(VehiclePricingSnapshot.serializer(), value)

    /** Returns null (rather than throwing) on any malformed/legacy-shape cached value. */
    fun decode(raw: String?): VehiclePricingSnapshot? {
        if (raw.isNullOrEmpty()) return null
        return try {
            json.decodeFromString(VehiclePricingSnapshot.serializer(), raw)
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * In-memory default so JVM tests/graphs that omit a real (platform DataStore-backed) cache still
 * compile and behave sanely — no persistence across process death, just enough for
 * [VehiclePricingRepository]'s constructor default.
 */
class InMemoryVehiclePricingCache : VehiclePricingCache {
    private val state = MutableStateFlow<VehiclePricingSnapshot?>(null)
    override val snapshot: Flow<VehiclePricingSnapshot?> = state.asStateFlow()

    override suspend fun write(snapshot: VehiclePricingSnapshot) {
        state.value = snapshot
    }
}
