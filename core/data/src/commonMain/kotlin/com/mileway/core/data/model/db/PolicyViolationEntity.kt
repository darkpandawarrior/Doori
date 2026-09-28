package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * L2 (MIGRATION_48_49): one policy-engine violation attached to a report or a specific claim line
 * at capture time (mirrors [com.mileway.core.data.domain.claim.ClaimLine.policyFlags], persisted
 * with the human-readable [message] a later lane's policy engine produces alongside the flag
 * [code]). [claimLineId] is null for a report-level violation (e.g. missing receipt above a
 * threshold that isn't tied to one line).
 */
@Entity(
    tableName = "policy_violations",
    indices = [Index(value = ["reportId"]), Index(value = ["claimLineId"])],
)
data class PolicyViolationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reportId: String,
    val claimLineId: String?,
    val code: String,
    val message: String,
    val createdAtMs: Long,
)
