package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V22 P6.4: a device session the demo account is signed in on — replaces
 * `stub.ProfileMockData.sessions()`'s bare in-memory list as the source of truth for
 * `ActiveSessionsScreen`'s revoke/bulk-sign-out-all-others actions, so a revoke actually persists
 * across process death instead of resetting on next launch.
 *
 * [isCurrent] marks the one row that can never be revoked (the device driving this app instance);
 * `status` (Active/Recent/Idle) is derived from [lastActiveMillis] at read time, not stored here,
 * so it never goes stale relative to "now."
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val deviceName: String,
    val platform: String,
    val lastActiveMillis: Long,
    val isCurrent: Boolean,
    // PLAN_V24 P7.2: device-detail enrichment (deviceType is derived from platform, not stored).
    val os: String = "",
    val appVersion: String = "",
    val ip: String = "",
)
