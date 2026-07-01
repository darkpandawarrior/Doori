package com.mileway.core.platform

import android.app.Activity

/*
 * Activity-scoped manager factories (KEY DECISION #2). Play-Core update/review need the host Activity
 * (not the Application context), so they cannot be plain Koin singletons. Each flavor binds the
 * matching factory (gms = real Play-Core impl; noGms = no-op), and the LocalManagerProvider androidMain
 * actual resolves the factory from Koin and calls create(activity) with LocalContext's Activity.
 *
 * The factory interfaces live here (not in :app) so core:ui's LocalManagerProvider actual can depend on
 * them without depending on a flavor source set.
 */

/** Creates an Activity-scoped [AppUpdateManager]. gms → Play-Core; noGms → no-op. */
fun interface AppUpdateManagerFactory {
    fun create(activity: Activity): AppUpdateManager
}

/** Creates an Activity-scoped [AppReviewManager]. gms → Play review; noGms → store-intent / no-op. */
fun interface AppReviewManagerFactory {
    fun create(activity: Activity): AppReviewManager
}
