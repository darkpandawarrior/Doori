package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * L2 (MIGRATION_48_49): a payout journal entry queued for a paid [reportId] — the local record a
 * later lane's payout/GL-export step reads and marks off, keyed independently of
 * [PendingPaymentJournalEntity.id] so a report can be re-journaled without clobbering a prior row
 * (e.g. a correction).
 */
@Entity(
    tableName = "pending_payment_journals",
    indices = [Index(value = ["reportId"])],
)
data class PendingPaymentJournalEntity(
    @PrimaryKey
    val id: String,
    val reportId: String,
    val amountMinor: Long,
    val currency: String,
    val glAccountCode: String?,
    val status: String,
    val createdAtMs: Long,
)
