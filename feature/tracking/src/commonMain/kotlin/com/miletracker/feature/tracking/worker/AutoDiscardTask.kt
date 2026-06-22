package com.miletracker.feature.tracking.worker

import com.miletracker.feature.tracking.repository.CurrentTrackRepository
import com.miletracker.feature.tracking.repository.LocationRepository
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import dev.brewkits.kmpworkmanager.background.domain.Worker
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult

/**
 * P-F.2: commonMain Worker — discards the active journey when the auto-discard
 * policy is enabled (daily cutoff flag, default off).
 *
 * [isEnabled] is injected as a suspend lambda so platform-specific settings
 * (DemoSettingsRepository on both Android and iOS) can be read without the
 * commonMain class depending on platform-specific types.
 */
class AutoDiscardTask(
    private val isEnabled: suspend () -> Boolean,
    private val savedTrackRepository: SavedTrackRepository,
    private val locationRepository: LocationRepository,
    private val currentTrackRepository: CurrentTrackRepository,
) : Worker {
    override suspend fun doWork(
        input: String?,
        env: WorkerEnvironment,
    ): WorkerResult {
        if (!isEnabled()) return WorkerResult.Success()
        val active = savedTrackRepository.getActiveTrack() ?: return WorkerResult.Success()
        locationRepository.deleteForToken(active.routeId)
        currentTrackRepository.clearSession()
        return WorkerResult.Success()
    }

    companion object {
        const val WORKER_CLASS = "AutoDiscardTask"
        const val TASK_ID = "com.miletracker.autodiscard"
        const val INTERVAL_MINUTES = 24L * 60
    }
}
