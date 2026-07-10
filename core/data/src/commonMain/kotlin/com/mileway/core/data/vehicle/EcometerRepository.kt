package com.mileway.core.data.vehicle

import com.mileway.core.data.dao.SavedTrackDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * PLAN_V24 P11.4 — the Ecometer aggregation source. Lives in core:data (not a feature module) so the
 * profile Eco-dashboard reads it without depending on feature:tracking. Computes CO₂ saved, fuel
 * cost saved, distance and trip count from the user's REAL completed trips
 * ([SavedTrackDao.getCompletedTracks]) × the seeded per-vehicle-type [emissionFactorFor] factors —
 * no fabricated totals. Offline: pure Room data.
 *
 * `SavedTrack.distance` is stored in metres; it is converted to km before feeding [computeEcometer].
 */
class EcometerRepository(
    private val savedTrackDao: SavedTrackDao,
) {
    fun observeTotals(): Flow<EcometerTotals> =
        savedTrackDao.getCompletedTracks().map { tracks ->
            computeEcometer(
                tracks.map { EcoTrip(vehicleKey = it.selectedVehicleType, distanceKm = it.distance / 1000.0) },
            )
        }
}
