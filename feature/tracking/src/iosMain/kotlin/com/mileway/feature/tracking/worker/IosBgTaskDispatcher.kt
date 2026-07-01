package com.mileway.feature.tracking.worker

import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Swift → KMP background-task bridge for iOS.
 *
 * On Android, `dev.brewkits.kmpworkmanager` runs registered [dev.brewkits.kmpworkmanager.background.domain.Worker]s
 * through WorkManager automatically (and its `DefaultAlarmReceiver` reschedules them after reboot — see P-F.3).
 * iOS has no such automatic hook: BGTask handlers must be registered in Swift `AppDelegate` and the handler is
 * responsible for executing the work. This object is the bridge the handler calls — it maps the Info.plist
 * BGTask identifier to the matching kmpworkmanager `Worker` (via [MilewayWorkerFactory], so the worker's Koin
 * dependencies resolve lazily at execution time), runs it, and invokes [onComplete] so Swift can call
 * `task.setTaskCompleted(success:)`.
 *
 * Swift call site (iosApp/AppDelegate.swift):
 * ```swift
 * BGTaskScheduler.shared.register(forTaskWithIdentifier: "com.mileway.maintenance", using: nil) { task in
 *     IosBgTaskDispatcher.shared.runTask(taskId: task.identifier) { success in
 *         task.setTaskCompleted(success: success)
 *     }
 *     // …reschedule…
 * }
 * ```
 *
 * An unknown identifier completes with `true` (nothing to do is not a failure).
 */
object IosBgTaskDispatcher {
    private const val TAG = "IosBgTaskDispatcher"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val factory = MilewayWorkerFactory()

    /** Maps a BGTask identifier (Info.plist) to the kmpworkmanager [Worker] class name. */
    private fun workerClassFor(taskId: String): String? =
        when (taskId) {
            MileageMaintenanceTask.TASK_ID -> MileageMaintenanceTask.WORKER_CLASS
            AutoDiscardTask.TASK_ID -> AutoDiscardTask.WORKER_CLASS
            else -> null
        }

    fun runTask(
        taskId: String,
        onComplete: (Boolean) -> Unit,
    ) {
        val workerClass = workerClassFor(taskId)
        if (workerClass == null) {
            Napier.w("No worker bound for BGTask id '$taskId' — completing as success (no-op)", tag = TAG)
            onComplete(true)
            return
        }
        scope.launch {
            try {
                val worker = factory.createWorker(workerClass)
                if (worker == null) {
                    onComplete(true)
                    return@launch
                }
                val result = worker.doWork(input = null, env = WorkerEnvironment(progressListener = null, isCancelled = { false }))
                onComplete(result is WorkerResult.Success)
            } catch (e: Exception) {
                Napier.e("BGTask '$taskId' failed", e, tag = TAG)
                onComplete(false)
            }
        }
    }
}
