package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * L2 (MIGRATION_48_49): a locked accounting period — once [periodKey] (e.g. `"2026-09"`) has a
 * row, no claim line dated inside it may be created or edited (enforced by a later lane's write
 * path, not by this table). No row means "open".
 */
@Entity(tableName = "period_locks")
data class PeriodLockEntity(
    @PrimaryKey
    val periodKey: String,
    val lockedAtMs: Long,
    val lockedBy: String,
)
