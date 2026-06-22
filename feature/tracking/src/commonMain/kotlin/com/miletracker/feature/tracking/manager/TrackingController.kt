package com.miletracker.feature.tracking.manager

/**
 * P-B.1: Platform-neutral command interface for the active tracking session. The ViewModel depends
 * on this interface; platform-specific plumbing (Android FGS Intent dispatch / iOS CoreLocation
 * session) is hidden behind it so VMs can live in commonMain.
 *
 * All methods are fire-and-forget (no return value): the live state is observed via
 * [com.miletracker.feature.tracking.service.TrackingServiceApi.trackingState].
 */
interface TrackingController {
    /** Start a new tracking session associated with [token]. */
    fun start(token: String)

    /** Pause the active session (accumulation paused, service stays alive). */
    fun pause(token: String)

    /** Resume a paused session. */
    fun resume(token: String)

    /** Stop and finalize the active session. */
    fun stop(token: String)
}
