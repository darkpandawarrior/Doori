package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * L2 (MIGRATION_48_49): the persisted row backing [com.mileway.core.data.domain.claim.Report].
 * [state] stores [com.mileway.core.data.domain.claim.ReportLifecycleState]'s enum name.
 * [recordVersion] is bumped on every local write ([ReportRepository]) — the same optimistic-
 * concurrency counter the contract type declares, kept durable here rather than re-derived.
 * The report's [com.mileway.core.data.domain.claim.ApprovalChain] is NOT duplicated onto this row —
 * [ApprovalStepEntity] rows for this [id] are the single source of truth for the chain, ordered by
 * `stepIndex`.
 */
@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey
    val id: String,
    val employeeId: String,
    val state: String,
    val recordVersion: Long,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val submittedAtMs: Long? = null,
    val accountingPeriodKey: String? = null,
)
