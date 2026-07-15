package com.mileway.feature.tracking.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vehiclePricingCacheDataStore by preferencesDataStore(name = "vehicle_pricing_cache")

/** Android actual for [VehiclePricingCache] (PLAN_V33 A6) — mirrors `SnapshotCacheStore`'s idiom. */
class VehiclePricingCacheStore(private val context: Context) : VehiclePricingCache {
    private val payloadKey = stringPreferencesKey("vehicle_pricing_snapshot_json")

    override val snapshot: Flow<VehiclePricingSnapshot?> =
        context.vehiclePricingCacheDataStore.data.map { prefs -> VehiclePricingCacheCodec.decode(prefs[payloadKey]) }

    override suspend fun write(snapshot: VehiclePricingSnapshot) {
        context.vehiclePricingCacheDataStore.edit { prefs ->
            prefs[payloadKey] = VehiclePricingCacheCodec.encode(snapshot)
        }
    }
}
