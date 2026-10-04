package com.mileway.core.data.claim

import com.mileway.core.data.dao.ClaimLineDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.ReportEntity
import com.mileway.core.data.model.db.SavedTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * L2: wraps every pre-existing *completed, non-discarded* `saved_tracks` row (the real legacy
 * mileage-trip table — [SavedTrackDao]/[SavedTrack]) into a shell [ReportEntity] + one mileage
 * [ClaimLineEntity], one-time and idempotent, gated by [BackfillMarker].
 *
 * Deliberately does NOT read `log_miles_drafts` ([com.mileway.core.data.dao.LogMilesDraftDao]): those
 * are in-progress, unsubmitted form drafts, not finished trips — wrapping one into a "shell report"
 * would misrepresent an incomplete draft as a claim. `submit_drafts` no longer exists at all
 * (MIGRATION_47_48 dropped it when the offline-outbox extraction moved that shape to its own
 * database) — the brief's third named table is gone by the time this migration lands, confirmed by
 * reading [com.mileway.core.data.database.Migrations].
 *
 * Idempotency is anchored on [ClaimLineEntity.sourceTripId]'s UNIQUE index (= the trip's
 * [SavedTrack.routeId]): [backfillOne] uses [ClaimLineDao.insertMileageDraft] to reserve the line and
 * write its report in one transaction. Re-running [run] — or L4's auto-draft later claiming the same
 * trip — is a no-op for a trip already
 * wrapped, without relying on a caught constraint-violation exception as control flow. [marker]
 * additionally short-circuits [run] entirely once the one-time pass has completed.
 */
class LegacyMileageBackfillWorker(
    private val savedTrackDao: SavedTrackDao,
    private val claimLineDao: ClaimLineDao,
    private val marker: BackfillMarker,
    private val json: Json,
    private val clock: Clock = Clock.System,
) {
    /** Returns the retryable failure count; callers can route row failures to their platform logger. */
    suspend fun run(
        logFailure: (String, Exception) -> Unit = { tripId, failure ->
            println("Legacy mileage backfill failed for trip $tripId: ${failure.stackTraceToString()}")
        },
    ): Int {
        if (marker.isDone()) return 0
        var failedRows = 0
        savedTrackDao
            .getCompletedTracks()
            .first()
            .filter { !it.isDiscarded }
            .forEach { track ->
                runCatching { backfillOne(track) }.onFailure { failure ->
                    if (failure is CancellationException || failure !is Exception) throw failure
                    failedRows++
                    logFailure(track.routeId, failure)
                }
            }
        if (failedRows == 0) {
            marker.markDone()
        }
        return failedRows
    }

    private suspend fun backfillOne(track: SavedTrack) {
        val sourceTripId = track.routeId

        val now = clock.now().toEpochMilliseconds()
        val reportId = "legacy_$sourceTripId"
        val employeeId = track.startedByEmployeeCode.ifBlank { track.startedByAccountId.orEmpty() }

        val report =
            ReportEntity(
                id = reportId,
                employeeId = employeeId,
                state = ReportLifecycleState.SUBMITTED.name,
                recordVersion = 1L,
                createdAtMs = track.createdAt.takeIf { it > 0 } ?: now,
                updatedAtMs = now,
            )
        // ponytail: amountMinor is a crude rupees->paise cast of whatever was recorded at
        // submission time (often 0 for older rows that predate a reliable submittedAmount write) —
        // good enough for a shell record a later lane can re-price; not a payout-accurate figure.
        val line =
            MileageLine(
                id = "legacy_line_$sourceTripId",
                amountMinor = (track.submittedAmount * MINOR_UNIT_SCALE).toLong().coerceAtLeast(0L),
                currency = track.submittedAmountCurrency.ifBlank { "INR" },
                sourceTripId = sourceTripId,
                distanceKm = track.distance / METRES_PER_KM,
                vehicleKey = track.selectedVehicleType,
            )
        claimLineDao.insertMileageDraft(report, line.toEntity(reportId, now, json))
    }

    private companion object {
        const val MINOR_UNIT_SCALE = 100
        const val METRES_PER_KM = 1_000.0
    }
}
