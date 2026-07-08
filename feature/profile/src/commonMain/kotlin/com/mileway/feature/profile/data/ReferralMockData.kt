package com.mileway.feature.profile.data

import com.mileway.core.data.referral.ReferralActivity
import com.mileway.core.data.referral.ReferralLeaderboardEntry
import com.mileway.core.data.referral.ReferralStatus
import com.mileway.core.data.referral.ReferralTxn

/**
 * PLAN_V24 P5.1: seed data for referral-program v2. Only [txns] is Room-backed (via
 * ReferralRepository.seedIfEmpty); the [leaderboard] and [activity] feeds are static demo fixtures.
 */
object ReferralMockData {
    /** Referred users at mixed stages so the hub exercises pending/completed buckets + the meter. */
    val txns: List<ReferralTxn> =
        listOf(
            ReferralTxn(
                id = "REF-001",
                refereeName = "Rahul Mehra",
                status = ReferralStatus.SUCCESS,
                taskMessage = "Completed 5 trips — reward credited",
                processedMoney = 250.0,
                processedCredits = 500,
                userNumRides = 5,
                nextTargetRides = 5,
                nextTargetMoney = 0.0,
                nextTargetCredits = 0,
            ),
            ReferralTxn(
                id = "REF-002",
                refereeName = "Priya Sharma",
                status = ReferralStatus.PENDING,
                taskMessage = "2 of 5 trips done — keep going!",
                processedMoney = 100.0,
                processedCredits = 200,
                userNumRides = 2,
                nextTargetRides = 5,
                nextTargetMoney = 150.0,
                nextTargetCredits = 300,
            ),
            ReferralTxn(
                id = "REF-003",
                refereeName = "Vikram Nair",
                status = ReferralStatus.PENDING,
                taskMessage = "Signed up — first trip pending",
                processedMoney = 0.0,
                processedCredits = 0,
                userNumRides = 0,
                nextTargetRides = 1,
                nextTargetMoney = 50.0,
                nextTargetCredits = 100,
            ),
            ReferralTxn(
                id = "REF-004",
                refereeName = "Asha Verma",
                status = ReferralStatus.PENDING,
                taskMessage = "4 of 5 trips done — almost there!",
                processedMoney = 200.0,
                processedCredits = 400,
                userNumRides = 4,
                nextTargetRides = 5,
                nextTargetMoney = 50.0,
                nextTargetCredits = 100,
            ),
            ReferralTxn(
                id = "REF-005",
                refereeName = "Sunita Pillai",
                status = ReferralStatus.FAILED,
                taskMessage = "Invite expired before first trip",
                processedMoney = 0.0,
                processedCredits = 0,
                userNumRides = 0,
                nextTargetRides = 1,
                nextTargetMoney = 0.0,
                nextTargetCredits = 0,
            ),
        )

    /** Seeded top-10 leaderboard with the signed-in user at rank 4. */
    val leaderboard: List<ReferralLeaderboardEntry> =
        listOf(
            ReferralLeaderboardEntry(1, "Deepa Nair", 42, 8_400),
            ReferralLeaderboardEntry(2, "Harish Reddy", 37, 7_200),
            ReferralLeaderboardEntry(3, "Anil Kumar", 29, 5_800),
            ReferralLeaderboardEntry(4, "You", 21, 4_200, isCurrentUser = true),
            ReferralLeaderboardEntry(5, "Meera Iyer", 18, 3_600),
            ReferralLeaderboardEntry(6, "Rohit Das", 15, 3_000),
            ReferralLeaderboardEntry(7, "Kavya Rao", 12, 2_400),
            ReferralLeaderboardEntry(8, "Sameer Khan", 9, 1_800),
            ReferralLeaderboardEntry(9, "Tara Menon", 6, 1_200),
            ReferralLeaderboardEntry(10, "Nikhil Joshi", 4, 800),
        )

    /** Seeded activity feed (the reference app `get_activity`). */
    val activity: List<ReferralActivity> =
        listOf(
            ReferralActivity("ACT-1", "Rahul Mehra completed 5 trips — you earned 500 credits", "2h ago"),
            ReferralActivity("ACT-2", "Asha Verma completed a trip — 4 of 5 done", "5h ago"),
            ReferralActivity("ACT-3", "Priya Sharma joined with your code", "1d ago"),
            ReferralActivity("ACT-4", "You climbed to rank 4 on the leaderboard", "2d ago"),
            ReferralActivity("ACT-5", "Vikram Nair signed up with your code", "3d ago"),
            ReferralActivity("ACT-6", "Sunita Pillai's invite expired", "5d ago"),
        )
}
