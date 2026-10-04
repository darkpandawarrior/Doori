package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * L2 (MIGRATION_48_49): the per-diem rate card a [com.mileway.core.data.domain.claim.PerDiemLine]
 * is computed against — one row per (region, grade). [id] is the composite natural key
 * `"<region>_<grade>"` (same single-string-PK idiom as [PeriodLockEntity]'s `periodKey`), so an
 * upsert-by-(region, grade) is a plain Room `@Insert(REPLACE)` with no separate lookup query.
 */
@Entity(tableName = "per_diem_rates")
data class PerDiemRateEntity(
    @PrimaryKey
    val id: String,
    val region: String,
    val grade: String,
    val dailyRateMinor: Long,
    val currency: String,
    val effectiveFromMs: Long,
)
