package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P5.3: a persisted scratch-card reward. [status] stores the `RewardStatus` enum name as
 * TEXT (converter-free enum-as-string). [grantedAtMs] orders the grid newest-first.
 */
@Entity(tableName = "reward_cards")
data class RewardCardEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val rewardLabel: String,
    val credits: Int,
    val status: String,
    val grantedAtMs: Long,
)
