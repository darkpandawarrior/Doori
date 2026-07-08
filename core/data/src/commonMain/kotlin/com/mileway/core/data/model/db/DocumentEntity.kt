package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P4.1: a persisted verification document (the reference app `DocRequirementResponse`, one row per
 * doc type). [docUrlsJson] and [docInfoJson] carry the `doc_url[]` / `doc_info[]` lists as
 * JSON-encoded `TEXT` (no `@TypeConverter`, the same convention as [VoucherEntity]'s
 * `expenseRouteIdsJson`). Enum-valued columns ([requirement]/[status]/[category]) store the enum
 * name as `TEXT` — the converter-free enum-as-string pattern already used across this schema.
 *
 * Kept a pure Room POJO on purpose: the JSON encode/decode lives in
 * [com.mileway.core.data.verification.DocumentCodec], not a companion here, because a companion
 * referencing a cross-package `@Serializable` type (`DocInfoField.serializer()`) makes Room's KSP
 * processor fail to resolve the entity ("[MissingType]").
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val docType: String,
    val docTypeText: String,
    val requirement: String,
    val status: String,
    val docCount: Int,
    val isEditable: Boolean,
    val docUrlsJson: String,
    val reason: String,
    val instructions: String,
    val galleryRestricted: Boolean,
    val category: String,
    val docInfoJson: String,
    val isDocInfoEditable: Boolean,
    val updatedAtMs: Long,
)
