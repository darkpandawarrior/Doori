package com.miletracker.core.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import java.util.concurrent.TimeUnit

/**
 * Android [BackgroundScheduler] backed by WorkManager.
 *
 * It enqueues a generic [PlatformBackgroundWorker] keyed by the unique schedule name; the worker
 * resolves the matching [BackgroundTask] from Koin (named qualifier) and runs it. This keeps the
 * scheduler decoupled from concrete worker classes. The iOS counterpart (Phase 4) uses BGTaskScheduler.
 */
class AndroidBackgroundScheduler(private val context: Context) : BackgroundScheduler {
    override fun schedulePeriodic(
        uniqueName: String,
        intervalMinutes: Long,
    ) {
        val request =
            PeriodicWorkRequestBuilder<PlatformBackgroundWorker>(intervalMinutes, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_TASK to uniqueName))
                .addTag(uniqueName)
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override fun cancel(uniqueName: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName)
    }

    companion object {
        const val KEY_TASK = "platform_task_name"
    }
}

/**
 * Generic worker instantiated by WorkManager's default factory (it has the canonical
 * `(Context, WorkerParameters)` constructor). It resolves the scheduled [BackgroundTask] from the
 * global Koin context by name — mirroring the existing `MileageMaintenanceWorker` KoinComponent pattern.
 */
class PlatformBackgroundWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {
    override suspend fun doWork(): Result {
        val name = inputData.getString(AndroidBackgroundScheduler.KEY_TASK) ?: return Result.failure()
        val task = getKoin().getOrNull<BackgroundTask>(named(name)) ?: return Result.success()
        return try {
            task.run()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
