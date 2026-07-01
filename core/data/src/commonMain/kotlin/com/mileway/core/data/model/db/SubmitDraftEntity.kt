package com.mileway.core.data.model.db

import androidx.room.Entity

// Durable trip-draft row. Composite PK: (formKey, uniqueKey), idempotent upsert.
@Entity(tableName = "submit_drafts", primaryKeys = ["formKey", "uniqueKey"])
data class SubmitDraftEntity(
    val formKey: String,
    val uniqueKey: String,
    val payloadJson: String,
    val status: String,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
