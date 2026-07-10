package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P12.6: one submitted vehicle self-audit (self-inspection) in the Garage. Multi-row —
 * keeps the per-vehicle audit history. [checkedItemsCsv] is the comma-separated set of checklist
 * item keys the driver confirmed with a photo; [note] is the optional issue note (and the demo's
 * reject marker for [com.mileway.core.data.review.SimulatedReviewEngine]). The verdict is not
 * stored — it is derived at read time from [submittedAtMs] + [note], so a submitted audit resolves
 * from Pending to Passed/Failed the next time it is observed (see the engine's doc).
 */
@Entity(tableName = "vehicle_audits")
data class VehicleAuditEntity(
    @PrimaryKey
    val id: String,
    val vehicleId: String,
    val submittedAtMs: Long,
    val checkedItemsCsv: String,
    val note: String,
)
