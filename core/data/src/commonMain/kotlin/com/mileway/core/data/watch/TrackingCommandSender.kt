package com.mileway.core.data.watch

/**
 * Sends a watch-initiated start/stop to the paired phone.
 *
 * An interface here rather than a direct call because the only implementation lives in
 * `wear/src/gms` (it needs Play Services' `MessageClient`), while the UI and ViewModel that want to
 * call it live in `wear/src/main`. Same split `WatchSyncBridge` already uses for the other
 * direction.
 *
 * **A noGms watch has no implementation of this at all**, and that is not a bug to paper over: with
 * no Data Layer there is no transport to the phone, so the watch genuinely cannot drive tracking.
 * Callers take this as a nullable dependency and hide the control when it is absent, rather than
 * showing a button that quietly does nothing.
 */
interface TrackingCommandSender {
    /** Starts a session under [token]. The caller owns the token and must reuse it to stop. */
    suspend fun sendStart(token: String)

    /**
     * Stops the session identified by [token].
     *
     * The token must be the one the *running* session was started with — `TrackingController.stop`
     * returns early when it does not match, so a guessed token stops nothing while looking like it
     * worked. That is why the live token travels to the watch in `WatchSyncPayload.activeToken`.
     */
    suspend fun sendStop(token: String)
}
