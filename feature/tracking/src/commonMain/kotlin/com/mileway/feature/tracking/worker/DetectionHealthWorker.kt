package com.mileway.feature.tracking.worker

import com.mileway.core.data.dao.NotificationDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.feature.tracking.health.DetectionHealthNudges
import dev.brewkits.kmpworkmanager.background.domain.Worker
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/** Daily local inbox check. Existing rows are never replaced, preserving their read state. */
class DetectionHealthWorker(
    private val savedTrackDao: SavedTrackDao,
    private val notificationDao: NotificationDao,
    clock: Clock = Clock.System,
) : Worker {
    private val nudges = DetectionHealthNudges(clock)

    override suspend fun doWork(
        input: String?,
        env: WorkerEnvironment,
    ): WorkerResult {
        val candidates = nudges.evaluate(savedTrackDao.getCompletedTracks().first())
        val existingIds = notificationDao.observeAll().first().map { it.id }.toSet()
        val newRows = candidates.filterNot { it.id in existingIds }
        if (newRows.isNotEmpty()) notificationDao.upsertAll(newRows)
        return WorkerResult.Success()
    }

    companion object {
        const val WORKER_CLASS = "DetectionHealthWorker"
        const val TASK_ID = "com.mileway.detectionhealth"
        const val INTERVAL_MINUTES = 24L * 60
    }
}
