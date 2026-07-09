package com.mileway.core.data.referral

/*
 * PLAN_V24 P5.1: referral-program v2 models, rebuilt from the reference app's referral records. Offline/mock —
 * these live in core:data (the base module the Room entity and every consumer reach), same seam
 * choice as the P4.1 verification models.
 */

/** A referred user's status lifecycle (per the reference app): PENDING → SUCCESS | FAILED. */
enum class ReferralStatus { PENDING, SUCCESS, FAILED }

/**
 * One referred user's progress. [userNumRides]/[nextTargetRides] drives the
 * "N of M trips done" progress meter; [processedMoney]/[processedCredits] are what's been earned so far,
 * [nextTargetMoney]/[nextTargetCredits] what the next milestone unlocks. [taskMessage] is the
 * source's human-readable status line.
 */
data class ReferralTxn(
    val id: String,
    val refereeName: String,
    val status: ReferralStatus,
    val taskMessage: String,
    val processedMoney: Double,
    val processedCredits: Int,
    val userNumRides: Int,
    val nextTargetRides: Int,
    val nextTargetMoney: Double,
    val nextTargetCredits: Int,
) {
    /** Fraction (0f..1f) of the way to the next ride target — 1f once the target is met/exceeded. */
    val targetProgress: Float
        get() = if (nextTargetRides <= 0) 1f else (userNumRides.toFloat() / nextTargetRides).coerceIn(0f, 1f)

    /** A completed referral has reached SUCCESS; everything else is still "pending" in the UI buckets. */
    val isCompleted: Boolean get() = status == ReferralStatus.SUCCESS
}

/** A leaderboard row (reference app leaderboards). [isCurrentUser] highlights the signed-in user's rank. */
data class ReferralLeaderboardEntry(
    val rank: Int,
    val name: String,
    val referrals: Int,
    val credits: Int,
    val isCurrentUser: Boolean = false,
)

/** An activity-feed event (reference app activity feed) — a dated one-line milestone. */
data class ReferralActivity(
    val id: String,
    val message: String,
    val relativeTime: String,
)
