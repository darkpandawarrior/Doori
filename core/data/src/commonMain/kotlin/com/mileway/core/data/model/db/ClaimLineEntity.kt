package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * L2 (MIGRATION_48_49): the persisted row backing one
 * [com.mileway.core.data.domain.claim.ClaimLine]. [type] is the same wire discriminator the
 * sealed interface's `@SerialName` declares ("expense"/"mileage"/"per_diem"/"advance"); [detailsJson]
 * is the *entire* [com.mileway.core.data.domain.claim.ClaimLine] round-tripped through its own
 * polymorphic `kotlinx.serialization` codec — one JSON column instead of a column per subtype per
 * field (ponytail: sparse per-subtype columns would mean N-1 always-null columns per row). The
 * flat columns alongside it (amountMinor/currency/.../sourceTripId/cardMatchId) are indexed/queryable
 * projections of that same JSON, not a second source of truth — [ReportRepository] always writes
 * both from the one domain object.
 *
 * [sourceTripId]'s UNIQUE index is the idempotency anchor both [LegacyMileageBackfillWorker] and
 * L4's auto-draft rely on: re-running either against a trip that already produced a line is a
 * constrained upsert, never a duplicate row. [cardMatchId] is L10's corporate-card match key —
 * indexed so a later lane's "find the line a card transaction already matched" query doesn't scan.
 */
@Entity(
    tableName = "claim_lines",
    indices = [
        Index(value = ["sourceTripId"], unique = true),
        Index(value = ["reportId"]),
        Index(value = ["cardMatchId"]),
    ],
)
data class ClaimLineEntity(
    @PrimaryKey
    val id: String,
    val reportId: String,
    val type: String,
    val amountMinor: Long,
    val currency: String,
    val fxRatePinnedAt: Long?,
    val policyFlagsCsv: String,
    val cardMatchId: String?,
    val sourceTripId: String?,
    val detailsJson: String,
    val createdAtMs: Long,
)
