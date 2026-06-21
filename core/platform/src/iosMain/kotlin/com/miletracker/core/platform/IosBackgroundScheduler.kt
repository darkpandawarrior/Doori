package com.miletracker.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow

/**
 * iOS periodic background work via BGTaskScheduler (F) — the counterpart to Android's WorkManager.
 * Submits a BGAppRefreshTaskRequest whose [uniqueName] must also be declared in Info.plist
 * (BGTaskSchedulerPermittedIdentifiers) and registered at launch; submission is otherwise a no-op the
 * OS rejects. iOS treats the interval as a *minimum* — the system decides actual run time.
 */
class IosBackgroundScheduler : BackgroundScheduler {
    @OptIn(ExperimentalForeignApi::class)
    override fun schedulePeriodic(
        uniqueName: String,
        intervalMinutes: Long,
    ) {
        val request = BGAppRefreshTaskRequest(uniqueName)
        request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(intervalMinutes * 60.0)
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
    }

    override fun cancel(uniqueName: String) {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(uniqueName)
    }
}
