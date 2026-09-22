package com.mileway.feature.logging.repository

import com.mileway.core.data.dao.LogMilesFrequentRouteDao
import com.mileway.core.data.model.db.LogMilesFrequentRouteEntity
import com.mileway.feature.logging.repository.LogMilesDraftRepository.Companion.decodeStops
import com.mileway.feature.logging.repository.LogMilesDraftRepository.Companion.encodeStops
import com.mileway.feature.logging.ui.model.LocationStop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Frequent-route cache (Wave 3 Log Miles): derives a stable [LogMilesFrequentRouteEntity.routeKey]
 * from a route's stop names, upserting a use-count bump on every submit so the most-repeated
 * commutes surface for one-tap retrace. Mirrors [LogMilesDraftRepository]'s stop encode/decode so
 * both stores share one JSON shape for [LocationStop] lists.
 */
class LogMilesFrequentRouteRepository(
    private val dao: LogMilesFrequentRouteDao,
) {
    /** Frequent routes, most-used first — the source for a one-tap retrace list. */
    fun topRoutes(limit: Int = 5): Flow<List<LogMilesFrequentRoute>> =
        dao.observeAllRoutes().map { rows ->
            rows.sortedByDescending { it.useCount }.take(limit).map { it.toFrequentRoute() }
        }

    /**
     * Records a submitted route: bumps [LogMilesFrequentRouteEntity.useCount] if [stops]' key was
     * already cached, otherwise inserts a new entry with count 1. [distanceKm] is refreshed to the
     * latest submission's value.
     */
    suspend fun recordSubmission(
        stops: List<LocationStop>,
        distanceKm: Double,
        nowMillis: Long,
    ) {
        if (stops.size < 2) return
        val routeKey = keyFor(stops)
        val existing = dao.getByKey(routeKey)
        dao.upsert(
            LogMilesFrequentRouteEntity(
                routeKey = routeKey,
                locationsJson = encodeStops(stops),
                distanceKm = distanceKm,
                useCount = (existing?.useCount ?: 0) + 1,
                lastUsedAt = nowMillis,
            ),
        )
    }

    companion object {
        /** Stable key for a route: ordered stop names, so retracing the same path re-hits one row. */
        fun keyFor(stops: List<LocationStop>): String = stops.joinToString("|") { it.entry.name }

        private fun LogMilesFrequentRouteEntity.toFrequentRoute(): LogMilesFrequentRoute =
            LogMilesFrequentRoute(
                routeKey = routeKey,
                stops = decodeStops(locationsJson),
                distanceKm = distanceKm,
                useCount = useCount,
            )
    }
}

/** A cached frequent route ready for one-tap retrace: pre-fills the form's stops on selection. */
data class LogMilesFrequentRoute(
    val routeKey: String,
    val stops: List<LocationStop>,
    val distanceKm: Double,
    val useCount: Int,
)
