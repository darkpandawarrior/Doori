package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P12.5: per-account progress through the interactive training tour. Single row per
 * account (keyed by [accountId], like `destination_mode`). [stepName] is the last-seen
 * [com.mileway.core.data.engagement.TourStep] name; [completed]/[skipped] are the two terminal
 * outcomes (mutually exclusive). Empty on first run — no row means "never started".
 */
@Entity(tableName = "tour_progress")
data class TourProgressEntity(
    @PrimaryKey
    val accountId: String,
    val stepName: String,
    val completed: Boolean,
    val skipped: Boolean,
    val updatedAtMs: Long,
)
