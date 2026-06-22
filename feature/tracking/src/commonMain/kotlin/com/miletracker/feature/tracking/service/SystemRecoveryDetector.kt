package com.miletracker.feature.tracking.service

import com.miletracker.feature.tracking.repository.SavedTrackRepository

/**
 * P-C.2: Shared gap classifier for L2 lifecycle recovery.
 *
 * Both Android and iOS call [handleIfSystemRecovery] when they detect a system-relaunch:
 * - Android: null intent in onStartCommand means the OS redelivered after terminating the FGS.
 * - iOS: significant-change relaunch (always bg, so [isSystemRelaunch] = true).
 *
 * On detection, writes [SavedTrackRepository.markFgTerminated] so the journey quality scorer
 * can deduct and the UI can surface the "Tracking was interrupted" badge.
 */
class SystemRecoveryDetector(private val trackRepository: SavedTrackRepository) {
    /**
     * If [isSystemRelaunch] is true and [token] is non-empty, records the termination and
     * returns true.  Call from the service/controller's resume path.
     */
    suspend fun handleIfSystemRecovery(
        token: String,
        isSystemRelaunch: Boolean,
    ): Boolean {
        if (!isSystemRelaunch || token.isEmpty()) return false
        trackRepository.markFgTerminated(token)
        return true
    }
}
