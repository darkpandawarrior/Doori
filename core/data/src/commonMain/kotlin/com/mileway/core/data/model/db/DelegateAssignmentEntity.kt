package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * L2 (MIGRATION_48_49): "approver [delegatorAccountId] delegates report-approval authority to
 * [delegateAccountId], within [scope], for [startsAtMs, expiresAtMs]." A NEW table, deliberately
 * not a reuse of the pre-existing `delegations` table ([DelegationEntity]): that table's
 * `delegateName` is a free-text display string (no `accountId`), and it has no delegator side at
 * all — it is PLAN_V22 P6.3's account-agnostic "My Delegations" display list, not an
 * approver-to-approver authority grant the claim-approval engine can look up by account id on
 * either side. See PLAN_V22 §2's Architecture note (also referenced on [DelegationEntity]) for why
 * the two "delegation" concepts are never merged.
 */
@Entity(
    tableName = "delegate_assignments",
    indices = [Index(value = ["delegatorAccountId"]), Index(value = ["delegateAccountId"])],
)
data class DelegateAssignmentEntity(
    @PrimaryKey
    val id: String,
    val delegatorAccountId: String,
    val delegateAccountId: String,
    val scope: String,
    val startsAtMs: Long,
    val expiresAtMs: Long,
    val isActive: Boolean,
    val createdAtMs: Long,
)
