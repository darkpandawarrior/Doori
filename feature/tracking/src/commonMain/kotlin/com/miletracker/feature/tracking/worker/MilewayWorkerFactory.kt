package com.miletracker.feature.tracking.worker

import com.miletracker.core.data.settings.DemoSettingsRepository
import com.miletracker.feature.tracking.repository.CurrentTrackRepository
import com.miletracker.feature.tracking.repository.LocationRepository
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import dev.brewkits.kmpworkmanager.background.domain.Worker
import dev.brewkits.kmpworkmanager.background.domain.WorkerFactory
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Cross-platform [WorkerFactory] passed to [dev.brewkits.kmpworkmanager.kmpWorkerModule].
 *
 * Uses [KoinComponent] so all dependencies are resolved lazily from the fully-loaded
 * Koin graph at task-execution time (not at module-registration time). No constructor
 * arguments needed — the factory is safe to instantiate before Koin starts.
 */
class MilewayWorkerFactory : WorkerFactory, KoinComponent {
    override fun createWorker(workerClassName: String): Worker? =
        when (workerClassName) {
            MileageMaintenanceTask.WORKER_CLASS ->
                MileageMaintenanceTask(locationDao = get())

            AutoDiscardTask.WORKER_CLASS ->
                AutoDiscardTask(
                    isEnabled = { get<DemoSettingsRepository>().settings.first().autoDiscardEnabled },
                    savedTrackRepository = get<SavedTrackRepository>(),
                    locationRepository = get<LocationRepository>(),
                    currentTrackRepository = get<CurrentTrackRepository>(),
                )

            else -> null
        }
}
