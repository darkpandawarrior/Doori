package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * L2 (MIGRATION_48_49): the GL (general-ledger) account a claim [category] posts to — the mapping
 * [PendingPaymentJournalEntity] reads at journal-creation time. [category] is the same free-text
 * category value [com.mileway.core.data.domain.claim.ExpenseLine.category] carries, kept as the
 * primary key so an upsert-by-category is a plain `@Insert(REPLACE)`.
 */
@Entity(tableName = "gl_mappings")
data class GlMappingEntity(
    @PrimaryKey
    val category: String,
    val glAccountCode: String,
    val costCenter: String?,
    val updatedAtMs: Long,
)
