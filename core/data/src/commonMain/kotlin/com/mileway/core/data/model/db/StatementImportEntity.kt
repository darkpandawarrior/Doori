package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * L2 (MIGRATION_48_49): one imported corporate-card statement batch (the L10 card-matching lane's
 * ingestion record) — [rowCount] transactions read from [fileName], [status] tracking the import's
 * own lifecycle (e.g. "IMPORTED"/"MATCHING"/"DONE"), not any one transaction's match state.
 */
@Entity(tableName = "statement_imports")
data class StatementImportEntity(
    @PrimaryKey
    val id: String,
    val source: String,
    val fileName: String,
    val importedAtMs: Long,
    val rowCount: Int,
    val status: String,
)
