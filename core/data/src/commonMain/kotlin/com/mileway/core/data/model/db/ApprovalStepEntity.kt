package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * L2 (MIGRATION_48_49): the persisted row backing one
 * [com.mileway.core.data.domain.claim.ApprovalStep] in a report's approval chain. [action] stores
 * [com.mileway.core.data.domain.claim.ApprovalAction]'s enum name. Ordered per-[reportId] by
 * [stepIndex] — the chain is reconstructed by reading all rows for a report and sorting.
 */
@Entity(
    tableName = "approval_steps",
    indices = [Index(value = ["reportId"])],
)
data class ApprovalStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reportId: String,
    val stepIndex: Int,
    val role: String,
    val thresholdMinor: Long?,
    val actedBy: String,
    val onBehalfOf: String?,
    val action: String,
    val comment: String?,
    val actedAtMillis: Long,
)
