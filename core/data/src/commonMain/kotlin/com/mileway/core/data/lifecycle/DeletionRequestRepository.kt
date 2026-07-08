package com.mileway.core.data.lifecycle

import com.mileway.core.data.dao.DeletionRequestDao
import com.mileway.core.data.model.db.DELETION_REQUEST_ID
import com.mileway.core.data.model.db.DeletionRequestEntity
import com.mileway.core.data.review.SimulatedReviewEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/** PLAN_V24 P7.1: lifecycle of an account-deletion request (source: `KEY_REQUESTED_FOR_ACCOUNT_DELETION`). */
enum class DeletionStatus { NONE, REQUESTED, PROCESSING }

/** The observable deletion state. [NONE] means no row / nothing pending. */
data class DeletionState(
    val status: DeletionStatus = DeletionStatus.NONE,
    val reason: String? = null,
    val requestedAtMs: Long = 0L,
)

private const val DELETION_SIM_DELAY_MS = 6_000L

/**
 * PLAN_V24 P7.1: Room-backed account-deletion request. A request is cancelable while [REQUESTED];
 * once the [SimulatedReviewEngine] has "reviewed" it (sim delay elapsed) [advance] moves it to
 * [PROCESSING] — past the cancel window. The actual persona wipe + sign-out is orchestrated by the
 * feature-layer ViewModel when it observes [PROCESSING] (this module holds no persona/session refs).
 */
class DeletionRequestRepository(
    private val dao: DeletionRequestDao,
    private val reviewEngine: SimulatedReviewEngine = SimulatedReviewEngine(),
    private val clock: Clock = Clock.System,
) {
    fun observe(): Flow<DeletionState> = dao.observe().map { it.toState() }

    /** Submit a deletion request (REPLACEs any prior row). [reason] is optional. */
    suspend fun request(reason: String?) {
        dao.upsert(
            DeletionRequestEntity(
                id = DELETION_REQUEST_ID,
                status = DeletionStatus.REQUESTED.name,
                reason = reason?.ifBlank { null },
                requestedAtMs = clock.now().toEpochMilliseconds(),
            ),
        )
    }

    /** Cancel a pending request — allowed only while [REQUESTED] (the UI hides Cancel once PROCESSING). */
    suspend fun cancel() {
        dao.clear()
    }

    /**
     * Move a [REQUESTED] row to [PROCESSING] once the sim review delay has elapsed. Idempotent; a
     * no-op for other states. Returns true if the row is now (or was already) [PROCESSING].
     */
    suspend fun advance(): Boolean {
        val row = dao.get() ?: return false
        val status = DeletionStatus.entries.firstOrNull { it.name == row.status } ?: DeletionStatus.NONE
        return when (status) {
            DeletionStatus.PROCESSING -> true
            DeletionStatus.REQUESTED -> {
                if (!reviewEngine.isReviewed(row.requestedAtMs, DELETION_SIM_DELAY_MS)) {
                    false
                } else {
                    dao.upsert(row.copy(status = DeletionStatus.PROCESSING.name))
                    true
                }
            }
            DeletionStatus.NONE -> false
        }
    }

    /** Clears the request row (after the wipe completes, returning the install to a clean state). */
    suspend fun clear() {
        dao.clear()
    }

    private fun DeletionRequestEntity?.toState(): DeletionState {
        if (this == null) return DeletionState()
        val status = DeletionStatus.entries.firstOrNull { it.name == status } ?: DeletionStatus.NONE
        return DeletionState(status = status, reason = reason, requestedAtMs = requestedAtMs)
    }
}
