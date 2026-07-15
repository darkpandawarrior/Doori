package com.mileway.feature.tracking.repository

import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.core.network.ReadState
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.screenStateStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VehiclePricingRepository(
    private val api: MilewayNetworkApi,
    // PLAN_V33 A6: defaulted so every existing JVM test / Koin graph that constructs this with a
    // single `api` arg keeps compiling; real graphs bind the platform DataStore/NSUserDefaults
    // actual (see VehiclePricingCacheStore).
    private val cache: VehiclePricingCache = InMemoryVehiclePricingCache(),
    // PLAN_V33 A6: defaulted to "always online" so JVM tests (and any graph that omits a real
    // connectivity check) never invoke Konnection's actual host-reachability probe — Konnection's
    // JVM/desktop actual pings real hosts, which is unsafe/slow inside a sandboxed test run and
    // would fire on every existing TrackMilesViewModel test via loadVehicles(). Real graphs
    // (TrackingModule) wire NetworkMonitor::isConnectedNow explicitly.
    private val isOnline: () -> Boolean = { true },
) {
    suspend fun getVehicles(trackMiles: Boolean = true): List<ApprovedVehicle> = api.vehicles(trackMiles).vehicles

    suspend fun getPricing(): Map<String, Double> = api.pricing().data

    /**
     * PLAN_V33 A6: offline-capable vehicles read — emits the last-cached list immediately (so the
     * Track Miles screen still renders vehicles with no network), then refreshes from the network
     * when online. A successful refresh writes both vehicles and pricing into [cache], which
     * re-emits the fresh [ReadState.Content].
     */
    fun vehiclesState(trackMiles: Boolean = true): Flow<ReadState<List<ApprovedVehicle>>> =
        screenStateStream(
            cache = cache.snapshot.map { it?.vehicles },
            isOnline = isOnline,
            refresh = {
                val vehicles = api.vehicles(trackMiles).vehicles
                val pricing = runCatching { api.pricing().data }.getOrDefault(emptyMap())
                cache.write(VehiclePricingSnapshot(vehicles, pricing))
            },
        )
}
