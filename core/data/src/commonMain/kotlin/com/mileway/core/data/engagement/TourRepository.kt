package com.mileway.core.data.engagement

import com.mileway.core.data.dao.TourProgressDao
import com.mileway.core.data.model.db.TourProgressEntity
import com.mileway.core.data.session.ActiveAccountSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/** Account key used for tour progress when no persona is signed in (guest). */
const val TOUR_GUEST_KEY: String = "guest"

/**
 * PLAN_V24 P12.5 — the per-account training-tour progress store. Persists the pure [TourState] (see
 * [TourStateMachine]) over Room, scoped to the active account like
 * [com.mileway.core.data.location.DestinationModeRepository]. Lives in core:data so both the tour
 * surface (feature:profile) and the badge board ([BadgeRepository], which lights the
 * [BadgeId.TOUR_COMPLETE] badge off [observeCompleted]) read it without a feature-to-feature dep.
 * Pure/offline — no backend.
 */
class TourRepository(
    private val dao: TourProgressDao,
    private val activeAccount: ActiveAccountSource,
    private val clock: Clock = Clock.System,
) {
    /** Live tour state for the active account (defaults to a fresh [TourState] when never started). */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observe(): Flow<TourState> =
        activeAccount.activeAccountId.flatMapLatest { accountId ->
            dao.observe(accountId ?: TOUR_GUEST_KEY).map { it.toState() }
        }

    /** Whether the active account has completed the tour — drives the tour-complete badge + first-run offer. */
    fun observeCompleted(): Flow<Boolean> = observe().map { it.status == TourStatus.COMPLETED }

    /** Advance the persisted tour one step (see [tourAdvance]). */
    suspend fun advance() = persist(tourAdvance(currentState()))

    /** Skip the persisted tour (see [tourSkip]). */
    suspend fun skip() = persist(tourSkip(currentState()))

    /** Restart the tour from the first step (re-entry). */
    suspend fun restart() = persist(tourRestart())

    private suspend fun currentState(): TourState = dao.get(activeAccountKey()).toState()

    private suspend fun persist(state: TourState) {
        dao.upsert(
            TourProgressEntity(
                accountId = activeAccountKey(),
                stepName = state.step.name,
                completed = state.status == TourStatus.COMPLETED,
                skipped = state.status == TourStatus.SKIPPED,
                updatedAtMs = clock.now().toEpochMilliseconds(),
            ),
        )
    }

    private suspend fun activeAccountKey(): String = activeAccount.activeAccountId.first() ?: TOUR_GUEST_KEY

    private fun TourProgressEntity?.toState(): TourState {
        if (this == null) return TourState()
        val step = TourStep.entries.firstOrNull { it.name == stepName } ?: TourStep.INTRO
        val status =
            when {
                completed -> TourStatus.COMPLETED
                skipped -> TourStatus.SKIPPED
                else -> TourStatus.IN_PROGRESS
            }
        return TourState(step = step, status = status)
    }
}
