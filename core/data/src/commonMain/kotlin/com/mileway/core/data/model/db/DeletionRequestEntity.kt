package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P7.1: the single account-deletion request row (source: the reference app/the reference app
 * `KEY_REQUESTED_FOR_ACCOUNT_DELETION`). [id] is the constant [DELETION_REQUEST_ID] so there is at
 * most one row; requesting REPLACEs it, cancelling clears it. [status] stores the `DeletionStatus`
 * enum name; [reason] is the optional user-entered note (also the SimulatedReviewEngine payload).
 */
@Entity(tableName = "deletion_request")
data class DeletionRequestEntity(
    @PrimaryKey
    val id: String,
    val status: String,
    val reason: String?,
    val requestedAtMs: Long,
)

/** The sole primary key used by [DeletionRequestEntity] — enforces a single request row. */
const val DELETION_REQUEST_ID = "current"
