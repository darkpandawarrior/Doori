package com.mileway.core.data.rewards

/*
 * PLAN_V24 P5.3: scratch-card reward models (the reference app `RewardsActivity` / `scratch_coupon`). Offline/
 * mock — in core:data (the base module the Room entity and every consumer reach), same seam choice
 * as the P5.1/P5.2 growth models.
 */

/** Whether a reward card has been scratched (revealed) yet. */
enum class RewardStatus { UNSCRATCHED, SCRATCHED }

/**
 * An earned scratch card. [rewardLabel] is the human reveal text ("₹50 credit"); [credits] is the
 * numeric value added on reveal. Granted by referral SUCCESS transitions (P5.1) or seeded.
 */
data class RewardCard(
    val id: String,
    val title: String,
    val rewardLabel: String,
    val credits: Int,
    val status: RewardStatus,
)
