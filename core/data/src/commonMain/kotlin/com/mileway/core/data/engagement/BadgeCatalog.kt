package com.mileway.core.data.engagement

/**
 * PLAN_V24 P12.1 — the achievement-badge model + the pure milestone math. Badges are EARNED locally
 * from the user's real completed trips (see [BadgeRepository]); there is no backend or fabricated
 * "profile-API" badge feed. The compliment/rating content below is the seeded feedback layer the
 * reference app populated from an API — here it is a fixed local seed, shown on the hub when
 * non-empty (rating hidden when ≤0), so the shape matches without a network.
 */
enum class BadgeId { FIRST_TRIP, TEN_TRIPS, HUNDRED_KM, WEEK_STREAK }

/** One achievement badge and whether the user has earned it yet. */
data class Badge(
    val id: BadgeId,
    val earned: Boolean,
)

/** One completed trip's contribution to milestone math: distance (km) and the local day it fell on. */
data class BadgeTrip(
    val distanceKm: Double,
    val dayEpoch: Long,
)

/** A seeded compliment (feedback badge) with its tally — display-only, shown when [count] > 0. */
data class Compliment(
    val id: String,
    val count: Int,
)

/** The full badge board rendered on the profile hub: earned badges + compliments + aggregate rating. */
data class BadgeBoard(
    val badges: List<Badge> = BadgeId.entries.map { Badge(it, earned = false) },
    val compliments: List<Compliment> = emptyList(),
    val rating: Double = 0.0,
)

/** Milliseconds per day, used to bucket a trip's `createdAt` into a UTC day for the streak count. */
const val MILLIS_PER_DAY: Long = 86_400_000L

/** Distance (km) that earns the [BadgeId.HUNDRED_KM] badge. */
const val HUNDRED_KM_THRESHOLD_KM: Double = 100.0

/** Consecutive tracked days that earn the [BadgeId.WEEK_STREAK] badge. */
const val WEEK_STREAK_DAYS: Int = 7

/** Seeded compliments (feedback badges). Fixed local content; the reference app's API-fed equivalent. */
val SEEDED_COMPLIMENTS: List<Compliment> =
    listOf(
        Compliment("clean_ride", 12),
        Compliment("on_time", 9),
        Compliment("safe_driving", 7),
        Compliment("great_navigation", 5),
    )

/** Seeded aggregate rating — shown only behind the `showRating` plugin and only when > 0. */
const val SEEDED_RATING: Double = 4.8

/**
 * The pure milestone rule: which badges [trips] have earned. First trip (≥1), ten trips (≥10),
 * hundred km (summed distance ≥ 100), and a 7-day streak (longest run of consecutive tracked days).
 * Negative distances are ignored. Unit-tested; [BadgeRepository] just feeds it real trip data.
 */
fun computeEarnedBadges(trips: List<BadgeTrip>): Set<BadgeId> {
    val earned = mutableSetOf<BadgeId>()
    if (trips.isNotEmpty()) earned += BadgeId.FIRST_TRIP
    if (trips.size >= 10) earned += BadgeId.TEN_TRIPS
    val totalKm = trips.sumOf { it.distanceKm.coerceAtLeast(0.0) }
    if (totalKm >= HUNDRED_KM_THRESHOLD_KM) earned += BadgeId.HUNDRED_KM
    if (longestDayStreak(trips.map { it.dayEpoch }) >= WEEK_STREAK_DAYS) earned += BadgeId.WEEK_STREAK
    return earned
}

/** The longest run of consecutive days present in [days] (each value is a day index). */
fun longestDayStreak(days: List<Long>): Int {
    if (days.isEmpty()) return 0
    val sorted = days.distinct().sorted()
    var best = 1
    var run = 1
    for (i in 1 until sorted.size) {
        run = if (sorted[i] == sorted[i - 1] + 1) run + 1 else 1
        if (run > best) best = run
    }
    return best
}
