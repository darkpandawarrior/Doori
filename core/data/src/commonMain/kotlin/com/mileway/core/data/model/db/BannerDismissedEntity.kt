package com.mileway.core.data.model.db

import androidx.room.Entity

/**
 * PLAN_V24 P13.1: one row per (account, banner) the user has dismissed. Composite primary key
 * isolates each persona's dismissals (respects the multi-profile isolation), a deliberate
 * improvement over the reference app's process-lifetime dismissal set — a dismissed banner stays
 * dismissed across app restarts. Empty on first run.
 */
@Entity(tableName = "banners_dismissed", primaryKeys = ["accountId", "bannerId"])
data class BannerDismissedEntity(
    val accountId: String,
    val bannerId: String,
    val dismissedAtMs: Long,
)
