package com.mileway.feature.tracking.worker

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.SavedTrackDao
import dev.brewkits.kmpworkmanager.background.domain.Worker
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult

/**
 * P-F.2: commonMain Worker — purges location rows older than 90 days.
 *
 * Wave-4 §2.3: also purges the GPS trail (locations table) for uploaded, completed, aged-out
 * trips (the same [SavedTrackDao.getRouteIdsEligibleForCleanup] set) and flips `has_local_data`
 * off on those routes, so [com.mileway.feature.tracking.repository.SavedTrackRepository.resolveLocalData]
 * can honestly report "purged, would fetch from server" instead of silently returning nothing.
 */
class MileageMaintenanceTask(
    private val locationDao: LocationDao,
    private val savedTrackDao: SavedTrackDao? = null,
) : Worker {
    override suspend fun doWork(
        input: String?,
        env: WorkerEnvironment,
    ): WorkerResult {
        val cutoff = kotlin.time.Clock.System.now().toEpochMilliseconds() - RETENTION_MS
        locationDao.deleteOlderThan(cutoff)

        savedTrackDao?.let { dao ->
            dao.getRouteIdsEligibleForCleanup(cutoff).forEach { routeId ->
                locationDao.deleteLocationsByToken(routeId)
                dao.markLocalDataPurged(routeId)
            }
        }
        return WorkerResult.Success()
    }

    companion object {
        const val WORKER_CLASS = "MileageMaintenanceTask"
        const val TASK_ID = "com.mileway.maintenance"
        const val RETENTION_MS = 90L * 24 * 60 * 60 * 1000
        const val INTERVAL_MINUTES = 7L * 24 * 60
    }
}
