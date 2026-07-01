package com.mileway.feature.tracking.service

import com.mileway.feature.tracking.repository.SavedTrackRepository

/**
 * P-C.3: commonMain policy that owns the "device shut down while tracking" flag.
 *
 * The flag is written by a platform-specific store (AndroidShutdownFlagStore on Android,
 * UserDefaults bridge on iOS) and consumed once here on the next app launch.
 *
 * @see AndroidShutdownFlagStore
 */
interface ShutdownFlagStore {
    fun set()

    /** Returns true if the flag was pending; always clears it (atomic consume-once). */
    fun consumeAndClear(): Boolean
}

class ShutdownFlagPolicy(
    private val store: ShutdownFlagStore,
    private val trackRepository: SavedTrackRepository,
) {
    /**
     * If the shutdown flag is pending, writes [SavedTrackRepository.markPhoneShutDown] for
     * [token] and clears the flag. Returns true when a shutdown was consumed, false otherwise.
     */
    suspend fun consumeAndMark(token: String): Boolean {
        if (token.isEmpty()) return false
        if (!store.consumeAndClear()) return false
        trackRepository.markPhoneShutDown(token)
        return true
    }
}
