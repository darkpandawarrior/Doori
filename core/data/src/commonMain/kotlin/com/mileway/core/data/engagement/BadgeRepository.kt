package com.mileway.core.data.engagement

import com.mileway.core.data.dao.SavedTrackDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * PLAN_V24 P12.1 — the badge-board source. Lives in core:data (not a feature module) so the profile
 * hub reads it without depending on feature:tracking, mirroring [EcometerRepository][com.mileway.core.data.vehicle.EcometerRepository].
 * Earned badges are derived from the user's REAL completed trips ([SavedTrackDao.getCompletedTracks])
 * via the pure [computeEarnedBadges] rule — no fabricated "earned" flags. Compliments/rating are the
 * fixed local seed (see [SEEDED_COMPLIMENTS]/[SEEDED_RATING]).
 *
 * `SavedTrack.distance` is stored in metres (converted to km); the streak buckets each trip's
 * `createdAt` into a UTC day. ponytail: UTC-day buckets, not the device's calendar day — good enough
 * for a demo streak; swap for a timezone-aware bucket if streaks ever gate real rewards.
 */
class BadgeRepository(
    private val savedTrackDao: SavedTrackDao,
) {
    fun observeBoard(): Flow<BadgeBoard> =
        savedTrackDao.getCompletedTracks().map { tracks ->
            val trips =
                tracks.map { track ->
                    BadgeTrip(
                        distanceKm = track.distance / 1000.0,
                        dayEpoch = track.createdAt / MILLIS_PER_DAY,
                    )
                }
            val earned = computeEarnedBadges(trips)
            BadgeBoard(
                badges = BadgeId.entries.map { Badge(it, earned = it in earned) },
                compliments = SEEDED_COMPLIMENTS,
                rating = SEEDED_RATING,
            )
        }
}
