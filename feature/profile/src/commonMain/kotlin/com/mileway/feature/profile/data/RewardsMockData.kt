package com.mileway.feature.profile.data

import com.mileway.core.data.rewards.RewardCard
import com.mileway.core.data.rewards.RewardStatus

/**
 * PLAN_V24 P5.3: seed set for scratch-card rewards — a mix of unscratched cards (to reveal) and one
 * already-scratched (showing the revealed state). Room-backed via RewardsRepository.seedIfEmpty.
 */
object RewardsMockData {
    val cards: List<RewardCard> =
        listOf(
            RewardCard("RWD-1", "Referral bonus", "₹50 credit", 500, RewardStatus.UNSCRATCHED),
            RewardCard("RWD-2", "Trip milestone", "₹25 credit", 250, RewardStatus.UNSCRATCHED),
            RewardCard("RWD-3", "Welcome gift", "₹100 credit", 1_000, RewardStatus.UNSCRATCHED),
            RewardCard("RWD-4", "Weekend special", "10% off coupon", 0, RewardStatus.UNSCRATCHED),
            RewardCard("RWD-5", "Loyalty reward", "₹75 credit", 750, RewardStatus.SCRATCHED),
        )
}
