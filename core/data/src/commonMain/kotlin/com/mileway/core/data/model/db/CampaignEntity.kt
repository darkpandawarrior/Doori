package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P5.4: a persisted marketing campaign. [status] stores the `CampaignStatus` enum name as
 * TEXT. [startedOnMs] orders the list newest-first (source: `startedOn` desc).
 */
@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val badge: String,
    val status: String,
    val mobileExclusive: Boolean,
    val contactEmail: String,
    val interestCaptured: Boolean,
    val startedOnMs: Long,
)
