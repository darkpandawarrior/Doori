package com.miletracker.feature.tracking.worker

import com.miletracker.core.data.dao.LocationDao
import dev.brewkits.kmpworkmanager.background.domain.Worker
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult

/** P-F.2: commonMain Worker — purges location rows older than 90 days. */
class MileageMaintenanceTask(private val locationDao: LocationDao) : Worker {
    override suspend fun doWork(
        input: String?,
        env: WorkerEnvironment,
    ): WorkerResult {
        val cutoff = kotlin.time.Clock.System.now().toEpochMilliseconds() - RETENTION_MS
        locationDao.deleteOlderThan(cutoff)
        return WorkerResult.Success()
    }

    companion object {
        const val WORKER_CLASS = "MileageMaintenanceTask"
        const val TASK_ID = "com.miletracker.maintenance"
        const val RETENTION_MS = 90L * 24 * 60 * 60 * 1000
        const val INTERVAL_MINUTES = 7L * 24 * 60
    }
}
