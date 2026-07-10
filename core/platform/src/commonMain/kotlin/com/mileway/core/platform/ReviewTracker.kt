package com.mileway.core.platform

import kotlin.time.Clock

/** Persistence for the review counters. Default in-memory; production can swap a DataStore-backed impl. */
interface ReviewStateStore {
    suspend fun load(): ReviewState

    suspend fun save(state: ReviewState)
}

/** Process-lifetime in-memory store (demo default; resets on cold start). */
class InMemoryReviewStateStore(initial: ReviewState = ReviewState()) : ReviewStateStore {
    private var current = initial

    override suspend fun load(): ReviewState = current

    override suspend fun save(state: ReviewState) {
        current = state
    }
}

/**
 * RV.4: drives the in-app review prompt from engagement signals. Records first-open + interaction counts,
 * and prompts (via the platform [AppReviewManager]) only when [ReviewEligibility] is satisfied, then stamps
 * the prompt time to enforce the cooldown.
 */
class ReviewTracker(
    private val store: ReviewStateStore = InMemoryReviewStateStore(),
    private val config: ReviewGateConfig = ReviewGateConfig(),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    suspend fun recordFirstOpenIfNeeded() {
        val state = store.load()
        if (state.firstOpenMillis <= 0L) store.save(state.copy(firstOpenMillis = now()))
    }

    suspend fun recordInteraction() {
        val state = store.load()
        store.save(state.copy(interactionCount = state.interactionCount + 1))
    }

    /** Prompts for review if eligible; returns true iff a prompt was launched. */
    suspend fun tryPrompt(manager: AppReviewManager): Boolean {
        val state = store.load()
        if (!ReviewEligibility.isEligible(state, now(), config)) return false
        manager.promptForReview()
        store.save(state.copy(lastPromptMillis = now()))
        return true
    }

    /**
     * PLAN_V24 P12.3: pure eligibility read for the native "Rate Mileway" sheet (no Play SDK) — the
     * caller shows its own UI when this is true, then calls [markPrompted]. Separate from [tryPrompt]
     * (which drives the platform [AppReviewManager]) so the offline demo can present a native sheet.
     */
    suspend fun shouldPrompt(): Boolean = ReviewEligibility.isEligible(store.load(), now(), config)

    /** Stamps the last-prompt time so the cooldown starts, after a native prompt was shown. */
    suspend fun markPrompted() {
        store.save(store.load().copy(lastPromptMillis = now()))
    }
}
