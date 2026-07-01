package com.mileway.feature.tracking.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mileway.core.data.dao.LocationDao
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Runs weekly to purge stale unsynced location rows older than 90 days. */
class MileageMaintenanceWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {
    private val locationDao: LocationDao by inject()

    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        locationDao.deleteOlderThan(cutoff)
        return Result.success()
    }

    companion object {
        const val TAG = "mileage_maintenance"
        private const val RETENTION_MS = 90L * 24 * 60 * 60 * 1000
    }
}
