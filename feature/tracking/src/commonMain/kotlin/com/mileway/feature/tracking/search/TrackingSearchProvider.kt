package com.mileway.feature.tracking.search

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchProvider
import com.mileway.core.data.search.SearchResult
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import kotlinx.coroutines.flow.first

private const val DAY_MS = 86_400_000L

/**
 * PLAN_V29 P29.S.1: the tracking module's contribution to master search (F0.5 registry) — 2 of the
 * 5 previously-dead providers, [SearchEntityType.MILEAGE] (Room-backed saved-trip log) and
 * [SearchEntityType.CHECKIN] (the same [LocationRepository] check-in points
 * `CheckInHistoryViewModel` reads). One class serving both types, not two — Koin's `single<T>` is
 * keyed by type with no qualifier, so a second `single<SearchProvider>` in the same module silently
 * overrides the first instead of adding to it; `getAll<SearchProvider>()` then only sees the last one.
 * Matches every other feature's one-provider-per-module shape (`ExpensesSearchProvider`,
 * `PayablesSearchProvider`, …).
 */
class TrackingSearchProvider(
    private val savedTracks: SavedTrackRepository,
    private val locations: LocationRepository,
) : SearchProvider {
    override val types: Set<SearchEntityType> = setOf(SearchEntityType.MILEAGE, SearchEntityType.CHECKIN)

    override suspend fun search(
        query: String,
        scope: SearchScope,
        filters: SearchFilters,
    ): List<SearchResult> {
        if (scope != SearchScope.EXPENSES && scope != SearchScope.VIEW_ALL) return emptyList()
        val q = query.trim()
        if (q.length < 2) return emptyList()

        val results = mutableListOf<SearchResult>()

        savedTracks.allTracksFlow().first()
            .filter { it.token.contains(q, true) || it.name?.contains(q, true) == true || it.service.contains(q, true) }
            .forEach { track ->
                results +=
                    SearchResult(
                        type = SearchEntityType.MILEAGE,
                        id = track.token,
                        title = track.name?.takeIf { it.isNotBlank() } ?: "Trip ${track.token}",
                        subtitle = "${track.getFormattedDistance()} · ${track.service.ifBlank { "Mileage" }}",
                        status = if (track.isSubmitted) "SUBMITTED" else track.getTrackingState().name,
                        amount = track.reimbursableAmount.takeIf { it > 0 },
                        dateEpochDay = track.startTime / DAY_MS,
                        deeplink = "mileway://track/${track.token}",
                    )
            }

        locations.allCheckInPoints().first()
            .filter { it.miscellaneous.contains(q, true) || it.reason?.contains(q, true) == true || it.checkInType.contains(q, true) }
            .forEach { point ->
                results +=
                    SearchResult(
                        type = SearchEntityType.CHECKIN,
                        id = point.id.toString(),
                        title = point.miscellaneous.ifBlank { if (point.checkInType == "MANUAL") "Manual check-in" else "Geo check-in" },
                        subtitle = point.reason?.takeIf { it.isNotBlank() } ?: point.checkInType,
                        status = point.checkInType,
                        dateEpochDay = point.date / DAY_MS,
                        deeplink = "mileway://track/checkin/${point.id}",
                    )
            }

        return if (filters.types.isEmpty()) results else results.filter { it.type in filters.types }
    }
}
