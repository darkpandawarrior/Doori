package com.mileway.feature.tracking.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

/**
 * iOS actual for [VehiclePricingCache] (PLAN_V33 A6). Unlike `SnapshotCacheStore`'s widget-sharing
 * concern, this cache is app-private only, so a plain `NSUserDefaults.standardUserDefaults`
 * (no App Group) is sufficient. `NSUserDefaults` has no native Flow, so reads are served from a
 * [MutableStateFlow] seeded on init and updated on every [write] — mirrors the read-your-own-write
 * expectation [com.mileway.core.network.screenStateStream] relies on (a refresh writes here, and
 * that write must re-emit through [snapshot]).
 */
class VehiclePricingCacheStore : VehiclePricingCache {
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
    private val state = MutableStateFlow(VehiclePricingCacheCodec.decode(defaults.stringForKey(PAYLOAD_KEY)))

    override val snapshot: Flow<VehiclePricingSnapshot?> = state.asStateFlow()

    override suspend fun write(snapshot: VehiclePricingSnapshot) {
        defaults.setObject(VehiclePricingCacheCodec.encode(snapshot), PAYLOAD_KEY)
        state.value = snapshot
    }

    private companion object {
        const val PAYLOAD_KEY = "vehicle_pricing_snapshot_json"
    }
}
